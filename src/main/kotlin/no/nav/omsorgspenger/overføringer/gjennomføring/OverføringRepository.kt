package no.nav.omsorgspenger.overføringer.gjennomføring

import kotliquery.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.NyOverføring
import no.nav.omsorgspenger.overføringer.fjernOverføringerUtenDager
import org.slf4j.LoggerFactory
import java.sql.Array
import java.time.LocalDate
import javax.sql.DataSource

internal class OverføringRepository(
    private val dataSource: DataSource) {

    internal fun hentBerørteSaksnummer(
        fra: Saksnummer,
        til: Saksnummer,
        fraOgMed: LocalDate) : Set<Saksnummer> {
        return using(sessionOf(dataSource)) { session ->
            session.berørteOverføringer(
                fra = fra,
                til = til,
                fraOgMed = fraOgMed
            ).saksnummer().minus(setOf(fra, til))
        }
    }

    internal fun hentOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjeldendeOverføringer> {
        return using(sessionOf(dataSource)) { session ->
            session.hentOverføringerMedOptionalStatus(
                saksnummer = saksnummer
            )
        }
    }

    internal fun gjennomførOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<NyOverføring>) : Map<Saksnummer, GjeldendeOverføringer> {
        val overføringerMedDager = overføringer.fjernOverføringerUtenDager()

        if (overføringerMedDager.isEmpty()) {
            return using(sessionOf(dataSource)) { it.hentAktiveOverføringer(
                saksnummer = setOf(fra, til)
            )}
        }

        val fraOgMed = overføringerMedDager.map { it.periode }.minByOrNull { it.fom }!!.fom

        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                val berørteOverføringer = transactionalSession.berørteOverføringer(
                    fra = fra,
                    til = til,
                    fraOgMed = fraOgMed
                ).utenOverføringer(
                    /**
                     * De eventuelle overføringene som finnes mellom de samme partene, men som har
                     * gått motsatt veg skal forbli urørte.
                     */
                    fra = til,
                    til = fra
                )

                val overlapper = berørteOverføringer.overlapper(
                    fraOgMed = fraOgMed
                )

                transactionalSession.endrePeriodePåOverføringer(
                    overføringer = overlapper,
                    fraOgMed = fraOgMed
                )

                transactionalSession.deaktiverOverføringer(
                    overføringer = berørteOverføringer.minus(overlapper)
                )

                transactionalSession.lagreNyeOverføringer(
                    fra = fra,
                    til = til,
                    overføringer = overføringerMedDager
                )
                transactionalSession.hentAktiveOverføringer(
                    saksnummer = berørteOverføringer.saksnummer().plus(setOf(fra, til))
                )
            }
        }
    }

    private fun Session.berørteOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        fraOgMed: LocalDate) : Set<OverføringDb> {

        val query = hentBerørteOverføringerQuery(
            fra = fra,
            til = til,
            fraOgMed = fraOgMed
        )

        return run(query.map { row ->
            row.somOverføringDb()
        }.asList).toSet()
    }

    private fun Session.endrePeriodePåOverføringer(
        overføringer: Set<OverføringDb>,
        fraOgMed: LocalDate) {
        if (overføringer.isEmpty()) return
        val nyTilOgMed = fraOgMed.minusDays(1)

        /**
         * De overføringene som overlapper, men den nye tilOgMed-datoen
         * gjør det til en ugylidig periode deaktiveres.
         */
        val deaktivert = overføringer.filter { overføring ->
            nyTilOgMed.isBefore(overføring.periode.fom)
        }.toSet().also { deaktiverOverføringer(it) }

        /**
         * Kun overføringene som overlapper, og med den nye tilOgMed-datoen
         * fortsat har en gyldig periode oppdateres.
         */
        val skalFåNyTilOgMed = overføringer.minus(deaktivert)
        val query = endreTilOgMedQuery(
            overføringIder = overføringIderArray(skalFåNyTilOgMed),
            nyTilOgMed = nyTilOgMed
        )
        run(query.asUpdate).also { oppdaterteRader -> if (oppdaterteRader != skalFåNyTilOgMed.size) {
            skalFåNyTilOgMed.map { it.id }.also {
                logger.warn("Forventet at overføringene $it skulle få tom=$nyTilOgMed, men $oppdaterteRader overføringer ble oppdatert.")
            }
        }}
    }

    private fun Session.deaktiverOverføringer(
        overføringer: Set<OverføringDb>) {
        if (overføringer.isEmpty()) return
        val query = deaktivertOverføringQuery(
            overføringIder = overføringIderArray(overføringer)
        )
        run(query.asUpdate).also { oppdaterteRader -> if (oppdaterteRader != overføringer.size) {
            overføringer.map { it.id }.also {
                logger.warn("Forventet at overføringene $it skulle få status=$Deaktivert, men $oppdaterteRader overføringer ble oppdatert.")
            }
        }}
    }

    private fun Session.lagreNyeOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<NyOverføring>) {
        overføringer.forEach { overføring ->
            run(lagreOverføringQuery(
                fra = fra,
                til = til,
                overføring = overføring
            ).asUpdate)
        }
    }

    private fun Session.hentAktiveOverføringer(
        saksnummer: Set<Saksnummer>
    ) = hentOverføringerMedOptionalStatus(
        saksnummer = saksnummer,
        status = Aktiv
    )

    private fun Session.hentOverføringerMedOptionalStatus(
        saksnummer: Set<Saksnummer>,
        status: String? = null
    ) : Map<Saksnummer, GjeldendeOverføringer> {
        val query = hentOverføringerQuery(
            saksnummer = saksnummerArray(saksnummer),
            status = status
        )
        val overføringer = run(query.map { row ->
            row.somOverføringDb()
        }.asList)

        val gjennomførteOverføringer = mutableMapOf<Saksnummer, GjeldendeOverføringer>()
        overføringer.saksnummer().forEach { sak ->
            gjennomførteOverføringer[sak] = GjeldendeOverføringer(
                fått = overføringer.filter { it.til == sak }.map { it.somGjeldendeOverføringFått() },
                gitt = overføringer.filter { it.fra == sak }.map { it.somGjeldendeOverføringGitt() }
            )
        }
        return gjennomførteOverføringer
    }

    private fun Session.saksnummerArray(saksnummer: Set<Saksnummer>) = createArrayOf("varchar", saksnummer)
    private fun Session.overføringIderArray(overføringer: Set<OverføringDb>) = createArrayOf("bigint", overføringer.map { it.id })

    private companion object {
        private val logger = LoggerFactory.getLogger(OverføringRepository::class.java)

        private const val Aktiv = "Aktiv"
        private const val Deaktivert = "Deaktivert"
        private const val Avslått = "Avslått"

        private const val HentOverføringerStatement =
            "SELECT * FROM overforing WHERE (fra = ANY(?) OR til = ANY(?))"
        private fun hentOverføringerQuery(saksnummer: Array, status: String?) =
            when (status) {
                null -> queryOf(HentOverføringerStatement, saksnummer, saksnummer)
                else -> queryOf("$HentOverføringerStatement AND status = ?", saksnummer, saksnummer, status)
            }

        private const val HentBerørteOverføringerStatement =
            "SELECT * FROM overforing WHERE (fra IN(?,?) OR til IN(?,?)) AND tom >= ? AND status = ?"
        private fun hentBerørteOverføringerQuery(fra: Saksnummer, til: Saksnummer, fraOgMed: LocalDate) =
            queryOf(HentBerørteOverføringerStatement, fra, til, fra, til, fraOgMed, Aktiv)

        private const val EndreTilOgMedStatement =
            "UPDATE overforing SET tom = ? WHERE id = ANY(?)"
        private fun endreTilOgMedQuery(overføringIder: Array, nyTilOgMed: LocalDate) =
            queryOf(EndreTilOgMedStatement, nyTilOgMed, overføringIder)

        private const val DeaktiverOverføringerStatement =
            "UPDATE overforing SET status = ? WHERE id = ANY(?)"
        private fun deaktivertOverføringQuery(overføringIder: Array) =
            queryOf(DeaktiverOverføringerStatement, Deaktivert, overføringIder)

        private const val LagreOverføringStatement =
            "INSERT INTO overforing (fom, tom, fra, til, antall_dager, status, lovanvendelser) VALUES(?,?,?,?,?,?,(to_json(?::json)))"
        private fun lagreOverføringQuery(fra: Saksnummer, til: Saksnummer, overføring: NyOverføring) =
            queryOf(
                statement = LagreOverføringStatement,
                overføring.periode.fom, overføring.periode.tom, fra, til, overføring.antallDager, Aktiv, "{}"
            )

        private fun Row.somPeriode() = Periode(
            fom = localDate("fom"),
            tom = localDate("tom")
        )

        private fun Row.somOverføringDb() = OverføringDb(
            id = long("id"),
            antallDager = int("antall_dager"),
            periode = somPeriode(),
            fra = string("fra"),
            til = string("til"),
            status = string("status")
        )

        private data class OverføringDb(
            val id: Long,
            // TODO: gjennomført
            val periode: Periode,
            val fra: Saksnummer,
            val til: Saksnummer,
            val antallDager: Int,
            val status: String) {

            private fun mapStatus() = when (status) {
                Aktiv -> GjeldendeOverføring.Status.Aktiv
                Avslått -> GjeldendeOverføring.Status.Avslått
                Deaktivert -> GjeldendeOverføring.Status.Deaktivert
                else -> GjeldendeOverføring.Status.Deaktivert.also {
                    logger.warn("Overføring $id har uventet status $status. Tolkes som $Deaktivert.")
                }
            }

            fun somGjeldendeOverføringFått() = GjeldendeOverføringFått(
                antallDager = antallDager,
                periode = periode,
                status = mapStatus(),
                fra = fra
            )
            fun somGjeldendeOverføringGitt() = GjeldendeOverføringGitt(
                antallDager = antallDager,
                periode = periode,
                status = mapStatus(),
                til = til
            )
        }

        private fun Set<OverføringDb>.overlapper(fraOgMed: LocalDate) =
            filter { it.periode.inneholder(fraOgMed) }.toSet()
        private fun Collection<OverføringDb>.saksnummer() : Set<Saksnummer> =
            map { listOf(it.fra, it.til) }.flatten().toSet()
        private fun Set<OverføringDb>.utenOverføringer(fra: Saksnummer, til: Saksnummer) =
            filterNot { it.fra == fra && it.til == til }.toSet()
    }
}