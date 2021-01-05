import kotliquery.*
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.GjennomførtOverføringer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.ZonedDateTime
import javax.sql.DataSource

internal class KoronaoverføringRepository(
    private val dataSource: DataSource) {

    internal fun gjennomførOverføringer(
        behovssekvensId: BehovssekvensId,
        fra: Saksnummer,
        til: Saksnummer,
        lovanvendelser: Lovanvendelser,
        antallDagerØnsketOverført: Int,
        overføringer: List<NyOverføring>): GjennomførtOverføringer {
        val berørteSaksnummer = setOf(fra, til)

        val overføringerDb = using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession -> {
                    overføringer.forEach { overføring ->
                        transactionalSession.lagreOverføring(
                            behovssekvensId = behovssekvensId,
                            fra = fra,
                            til = til,
                            lovanvendelser = lovanvendelser,
                            antallDagerØnsketOverført = antallDagerØnsketOverført,
                            overføring = overføring
                        )
                    }
                }
            }
            session.hentOverføringer(berørteSaksnummer)
        }

        return GjennomførtOverføringer(
            gjeldendeOverføringer = overføringerDb.somGjeldendeOverføringer(),
            berørteSaksnummer = berørteSaksnummer
        )
    }

    private fun Session.saksnummerArray(saksnummer: Set<Saksnummer>) = createArrayOf("varchar", saksnummer)
    private fun Session.lagreOverføring(
        behovssekvensId: BehovssekvensId,
        fra: Saksnummer,
        til: Saksnummer,
        lovanvendelser: Lovanvendelser,
        antallDagerØnsketOverført: Int,
        overføring: NyOverføring) {
        update(lagreOverføringQuery(
            behovssekvensId = behovssekvensId,
            fra = fra,
            til = til,
            overføring = overføring,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = antallDagerØnsketOverført
        ))
    }

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
        private const val LagreOverføringStatement =
            """
               INSERT INTO koronaoverforing (behovssekvens_id, fom, tom, fra, til, antall_dager, antall_dager_onsket_overfort, lovanvendelser) 
               VALUES(:behovssekvens_id, :fom, :tom, :fra, :til, :antall_dager, :antall_dager_onsket_overfort, :lovanvendelser ::jsonb) 
            """

        private fun lagreOverføringQuery(
            behovssekvensId: BehovssekvensId, fra: Saksnummer, til: Saksnummer, overføring: NyOverføring, lovanvendelser: Lovanvendelser, antallDagerØnsketOverført: Int) =
            queryOf(
                statement = LagreOverføringStatement,
                paramMap = mapOf(
                    "behovssekvensId" to behovssekvensId,
                    "fom" to overføring.periode.fom,
                    "tom" to overføring.periode.tom,
                    "fra" to fra,
                    "til" to til,
                    "antall_dager" to overføring.antallDager,
                    "antall_dager_onsket_overfort" to antallDagerØnsketOverført,
                    "lovanvendelser" to lovanvendelser.somJson()
                )
            )

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
