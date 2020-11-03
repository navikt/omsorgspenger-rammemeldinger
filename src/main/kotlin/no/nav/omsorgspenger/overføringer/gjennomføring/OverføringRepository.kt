package no.nav.omsorgspenger.overføringer.gjennomføring

import kotliquery.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.Overføring
import no.nav.omsorgspenger.overføringer.fjernOverføringerUtenDager
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
                fraOgMed = fraOgMed).saksnummer()
        }
    }

    internal fun hentOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjennomførteOverføringer> {
        return using(sessionOf(dataSource)) { session ->
            session.hentOverføringerMedOptionalStatus(
                saksnummer = saksnummer
            )
        }
    }

    internal fun gjennomførOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<Overføring>) : Map<Saksnummer, GjennomførteOverføringer> {
        val overføringerMedDager = overføringer.fjernOverføringerUtenDager()
        val fraOgMed = overføringerMedDager.map { it.periode }.minByOrNull { it.fom }!!.fom
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                val berørteOverføringer = transactionalSession.berørteOverføringer(
                    fra = fra,
                    til = til,
                    fraOgMed = fraOgMed
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
                    saksnummer = berørteOverføringer.saksnummer().plus(fra).plus(til)
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
        // TODO: Om eksisterende overføring har fom == tom vil dette produsere en overføring med ugyldig periode
        val query = endreTilOgMedQuery(
            overføringer = overføringer,
            nyTilOgMed = fraOgMed.minusDays(1)
        )
        // TODO: Logge om vi ikke får samme antall oppdatere rader som antall overføringer.
        run(query.asUpdate)
    }

    private fun Session.deaktiverOverføringer(
        overføringer: Set<OverføringDb>) {
        if (overføringer.isEmpty()) return
        val query = deaktivertOverføringQuery(
            overføringer = overføringer
        )
        // TODO: Logge om vi ikke får samme antall oppdaterte rader som antall overføringer
        run(query.asUpdate)
    }

    private fun Session.lagreNyeOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<Overføring>) {
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
    ) : Map<Saksnummer, GjennomførteOverføringer> {
        val query = hentOverføringerQuery(
            saksnummer = saksnummer,
            status = status
        )
        val overføringer = run(query.map { row ->
            row.somOverføringDb()
        }.asList)

        val gjennomførteOverføringer = mutableMapOf<Saksnummer, GjennomførteOverføringer>()
        overføringer.saksnummer().forEach { saksnummer ->
            gjennomførteOverføringer[saksnummer] = GjennomførteOverføringer(
                fått = overføringer.filter { it.til == saksnummer }.map { it.somGjennomførtOverføringFått() },
                gitt = overføringer.filter { it.fra == saksnummer }.map { it.somGjennomførtOverføringGitt() }
            )
        }
        return gjennomførteOverføringer
    }

    private companion object {
        private const val Aktiv = "Aktiv"
        private const val Deaktivet = "Deaktivet"
        private const val Avslått = "Avslått"

        private const val HentOverføringerStatement =
            "SELECT * FROM overforing WHERE fra IN(?) OR til IN(?)"
        private fun hentOverføringerQuery(saksnummer: Set<Saksnummer>, status: String?) =
            when (status) {
                null -> queryOf(HentOverføringerStatement, saksnummer, saksnummer)
                else -> queryOf("$HentOverføringerStatement AND status = ?", saksnummer, saksnummer, status)
            }

        private const val HentBerørteOverføringerStatement =
            "SELECT id, fom, tom, fra, til FROM overforing WHERE fra IN(?,?) OR til IN(?,?) AND tom >= ? AND status = ?"
        private fun hentBerørteOverføringerQuery(fra: Saksnummer, til: Saksnummer, fraOgMed: LocalDate) =
            queryOf(HentBerørteOverføringerStatement, fra, til, fra, til, fraOgMed, Aktiv)

        private const val EndreTilOgMedStatement =
            "UPDATE overforing SET tom = ? WHERE id in(?)"
        private fun endreTilOgMedQuery(overføringer: Set<OverføringDb>, nyTilOgMed: LocalDate) =
            queryOf(EndreTilOgMedStatement, overføringer.map { it.id }, nyTilOgMed)

        private const val DeaktiverOverføringerStatement =
            "UPDATE overforing SET status = ? WHERE id in(?)"
        private fun deaktivertOverføringQuery(overføringer: Set<OverføringDb>) =
            queryOf(DeaktiverOverføringerStatement, "Deaktivert", overføringer.map { it.id })

        private const val LagreOverføringStatement =
            "INSERT INTO overforing (fom, tom, fra, til, antall_dager, status, lovanvendelser) VALUES(?,?,?,?,?,?,(to_json(?::json)))"
        private fun lagreOverføringQuery(fra: Saksnummer, til: Saksnummer, overføring: Overføring) =
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
            val periode: Periode,
            val fra: Saksnummer,
            val til: Saksnummer,
            val antallDager: Int,
            val status: String) {
            private fun somGjennomførtOverføring(saksnummerMotpart: Saksnummer, type: GjennomførtOverføring.Type) = GjennomførtOverføring(
                antallDager = antallDager,
                periode = periode,
                status = when (status) {
                    Aktiv -> GjennomførtOverføring.Status.Aktiv
                    Avslått -> GjennomførtOverføring.Status.Avslått
                    else -> GjennomførtOverføring.Status.Deaktivert // TODO: Logg om det er en ukjent status..
                },
                saksnummerMotpart = saksnummerMotpart,
                type = type
            )
            fun somGjennomførtOverføringFått() = somGjennomførtOverføring(
                saksnummerMotpart = fra,
                type = GjennomførtOverføring.Type.Fått
            )
            fun somGjennomførtOverføringGitt() = somGjennomførtOverføring(
                saksnummerMotpart = til,
                type = GjennomførtOverføring.Type.Gitt
            )
        }

        private fun Set<OverføringDb>.overlapper(fraOgMed: LocalDate) =
            filter { it.periode.inneholder(fraOgMed) }.toSet()
        private fun Collection<OverføringDb>.saksnummer() : Set<Saksnummer> =
            map { listOf(it.fra, it.til) }.flatten().toSet()
    }
}