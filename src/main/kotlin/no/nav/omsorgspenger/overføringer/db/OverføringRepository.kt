package no.nav.omsorgspenger.overføringer.db

import kotliquery.*
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringLogg.hentOverføringLogger
import org.slf4j.LoggerFactory
import java.sql.Array
import java.time.ZonedDateTime
import javax.sql.DataSource

internal class OverføringRepository(
    private val dataSource: DataSource) {

    internal fun hentAktiveOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjeldendeOverføringer> {
        return using(sessionOf(dataSource)) { session -> session.hentOverføringerMedOptionalStatus(
            saksnummer = saksnummer,
            status = Aktiv,
            medKilder = true,
            medLovanvendelser = true
        )}
    }

    private fun Session.hentOverføringerMedOptionalStatus(
        saksnummer: Set<Saksnummer>,
        status: String? = null,
        medKilder: Boolean = false,
        medLovanvendelser: Boolean = false
    ) : Map<Saksnummer, GjeldendeOverføringer> {
        val query = hentOverføringerQuery(
            saksnummer = saksnummerArray(saksnummer),
            status = status
        )
        val overføringer = run(query.map { row ->
            row.somOverføringDb()
        }.asList)

        val overføringsLogger = when (medKilder) {
            true -> hentOverføringLogger(
                overføringIder = overføringIderArray(overføringer)
            )
            false -> listOf()
        }

        val gjennomførteOverføringer = mutableMapOf<Saksnummer, GjeldendeOverføringer>()
        overføringer.saksnummer().forEach { sak ->
            gjennomførteOverføringer[sak] = GjeldendeOverføringer(
                fått = overføringer.filter { it.til == sak }.map {
                    it.somGjeldendeOverføringFått(
                        medLovanvendelser = medLovanvendelser,
                        logg = overføringsLogger.filter { logg -> logg.overføringId == it.id }
                    )},
                gitt = overføringer.filter { it.fra == sak }.map {
                    it.somGjeldendeOverføringGitt(
                        medLovanvendelser = medLovanvendelser,
                        logg = overføringsLogger.filter { logg -> logg.overføringId == it.id }
                    )}
            )
        }
        return gjennomførteOverføringer
    }

    private fun Session.saksnummerArray(saksnummer: Set<Saksnummer>) = createArrayOf("varchar", saksnummer)
    private fun Session.overføringIderArray(overføringer: Collection<OverføringDb>) = createArrayOf("bigint", overføringer.map { it.id })

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

        private fun Row.somPeriode() = Periode(
            fom = localDate("fom"),
            tom = localDate("tom")
        )

        private fun Row.somOverføringDb() = OverføringDb(
            id = long("id"),
            gjennomført = zonedDateTime("gjennomfort"),
            antallDager = int("antall_dager"),
            periode = somPeriode(),
            fra = string("fra"),
            til = string("til"),
            status = string("status"),
            lovanvendelser = Lovanvendelser.fraJson(string("lovanvendelser")),
            antallDagerØnsketOverført = int("antall_dager_onsket_overfort")
        )

        private data class OverføringDb(
            val id: Long,
            val gjennomført: ZonedDateTime,
            val periode: Periode,
            val fra: Saksnummer,
            val til: Saksnummer,
            val antallDager: Int,
            val antallDagerØnsketOverført: Int,
            val status: String,
            val lovanvendelser: Lovanvendelser) {

            private fun mapStatus() = when (status) {
                Aktiv -> GjeldendeOverføring.Status.Aktiv
                Avslått -> GjeldendeOverføring.Status.Avslått
                Deaktivert -> GjeldendeOverføring.Status.Deaktivert
                else -> GjeldendeOverføring.Status.Deaktivert.also {
                    logger.warn("Overføring $id har uventet status $status. Tolkes som $Deaktivert.")
                }
            }

            fun somGjeldendeOverføringFått(
                logg: List<OverføringLogg.OverføringLoggDb>,
                medLovanvendelser: Boolean) = GjeldendeOverføringFått(
                gjennomført = gjennomført,
                antallDager = antallDager,
                periode = periode,
                status = mapStatus(),
                fra = fra,
                kilder = logg.somKilder(),
                lovanvendelser = when (medLovanvendelser) {
                    true -> lovanvendelser
                    false -> null
                }
            )
            fun somGjeldendeOverføringGitt(
                logg: List<OverføringLogg.OverføringLoggDb>,
                medLovanvendelser: Boolean) = GjeldendeOverføringGitt(
                gjennomført = gjennomført,
                antallDager = antallDager,
                antallDagerØnsketOverført = antallDagerØnsketOverført,
                periode = periode,
                status = mapStatus(),
                til = til,
                kilder = logg.somKilder(),
                lovanvendelser = when (medLovanvendelser) {
                    true -> lovanvendelser
                    false -> null
                }
            )
        }

        private fun List<OverføringLogg.OverføringLoggDb>.somKilder() = map { Kilde.internKilde(
            behovssekvensId = it.behovssekvensId,
            type = "Overføring"
        )}.toSet()

        private fun Collection<OverføringDb>.saksnummer() : Set<Saksnummer> =
            map { listOf(it.fra, it.til) }.flatten().toSet()
    }
}