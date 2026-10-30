package no.nav.omsorgspenger.koronaoverføringer.db

import kotliquery.*
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.ZonedDateTime
import javax.sql.DataSource

internal class KoronaoverføringRepository(
    private val dataSource: DataSource) {

    internal fun hentAlleOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjeldendeOverføringer> {
        return using(sessionOf(dataSource)) { session ->
            session.hentOverføringer(saksnummer).somGjeldendeOverføringer()
        }
    }


    private fun Session.saksnummerArray(saksnummer: Set<Saksnummer>) = createArrayOf("varchar", saksnummer)

    private fun Session.hentOverføringer(saksnummer: Set<Saksnummer>) : List<OverføringDb> {
        val query = hentOverføringerQuery(
            saksnummer = saksnummerArray(saksnummer)
        )
        return run(query.map { row ->
            row.somOverføringDb()
        }.asList)
    }

    private companion object {
        @Language("PostgreSQL")
        private const val HentOverføringerStatement =
            """
                SELECT * FROM koronaoverforing 
                WHERE (fra = ANY(:saksnummer) OR til = ANY(:saksnummer)) 
                AND status = 'Aktiv'
            """

        private fun hentOverføringerQuery(saksnummer: Array) =
            queryOf(
                statement = HentOverføringerStatement,
                paramMap = mapOf(
                    "saksnummer" to saksnummer
                )
            )

        private fun Row.somPeriode() = Periode(
            fom = localDate("fom"),
            tom = localDate("tom")
        )

        private fun Row.somOverføringDb() = OverføringDb(
            behovssekvensId = string("behovssekvens_id"),
            gjennomført = zonedDateTime("gjennomfort"),
            antallDager = int("antall_dager"),
            periode = somPeriode(),
            fra = string("fra"),
            til = string("til"),
            lovanvendelser = Lovanvendelser.fraJson(string("lovanvendelser")),
            antallDagerØnsketOverført = int("antall_dager_onsket_overfort")
        )

        private data class OverføringDb(
            val behovssekvensId: BehovssekvensId,
            val gjennomført: ZonedDateTime,
            val periode: Periode,
            val fra: Saksnummer,
            val til: Saksnummer,
            val antallDager: Int,
            val antallDagerØnsketOverført: Int,
            val lovanvendelser: Lovanvendelser) {
            fun somGjeldendeOverføringFått() = GjeldendeOverføringFått(
                gjennomført = gjennomført,
                antallDager = antallDager,
                periode = periode,
                status = GjeldendeOverføring.Status.Aktiv,
                fra = fra,
                kilder = setOf(Kilde.internKilde(
                    behovssekvensId = behovssekvensId,
                    type = "KoronaOverføring"
                )),
                lovanvendelser = lovanvendelser
            )
            fun somGjeldendeOverføringGitt() = GjeldendeOverføringGitt(
                gjennomført = gjennomført,
                antallDager = antallDager,
                antallDagerØnsketOverført = antallDagerØnsketOverført,
                periode = periode,
                status = GjeldendeOverføring.Status.Aktiv,
                til = til,
                kilder = setOf(Kilde.internKilde(
                    behovssekvensId = behovssekvensId,
                    type = "KoronaOverføring"
                )),
                lovanvendelser = lovanvendelser
            )
        }

        private fun List<OverføringDb>.somGjeldendeOverføringer(): Map<Saksnummer, GjeldendeOverføringer> {
            val gjennomførteOverføringer = mutableMapOf<Saksnummer, GjeldendeOverføringer>()
            saksnummer().forEach { sak ->
                gjennomførteOverføringer[sak] = GjeldendeOverføringer(
                    fått = filter { it.til == sak }.map { it.somGjeldendeOverføringFått() },
                    gitt = filter { it.fra == sak }.map { it.somGjeldendeOverføringGitt() }
                )
            }
            return gjennomførteOverføringer
        }

        private fun Collection<OverføringDb>.saksnummer() : Set<Saksnummer> =
            map { listOf(it.fra, it.til) }.flatten().toSet()
    }
}
