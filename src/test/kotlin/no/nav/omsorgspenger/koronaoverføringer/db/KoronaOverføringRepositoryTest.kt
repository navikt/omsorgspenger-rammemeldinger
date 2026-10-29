package no.nav.omsorgspenger.koronaoverføringer.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.JobberINorge
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class KoronaOverføringRepositoryTest(
    private val dataSource: DataSource
) {
    private val repository = KoronaoverføringRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `henter koronaoverføringer med begge parter og historiske opplysninger`() {
        leggInnOverføring("1", Ola, Kari, 10, 30, periode1)
        leggInnOverføring("2", Ola, Trond, 15, 15, periode1)
        leggInnOverføring("3", Trond, Kari, 6, 13, periode2)

        val overføringer = repository.hentAlleOverføringer(setOf(Ola, Kari, Trond)).medUtcTidssone()

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari, Trond)
        assertThat(overføringer.getValue(Ola).gitt).containsExactlyInAnyOrder(
            gitt("1", 10, 30, periode1, Kari),
            gitt("2", 15, 15, periode1, Trond)
        )
        assertThat(overføringer.getValue(Ola).fått).isEmpty()
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
        assertThat(overføringer.getValue(Kari).fått).containsExactlyInAnyOrder(
            fått("1", 10, periode1, Ola),
            fått("3", 6, periode2, Trond)
        )
        assertThat(overføringer.getValue(Trond).gitt).containsExactly(
            gitt("3", 6, 13, periode2, Kari)
        )
        assertThat(overføringer.getValue(Trond).fått).containsExactly(
            fått("2", 15, periode1, Ola)
        )
    }

    @Test
    fun `henter bare overføringer der etterspurt saksnummer er giver eller mottaker`() {
        leggInnOverføring("1", Ola, Kari, 10, 30, periode1)
        leggInnOverføring("2", Trond, Ola, 6, 13, periode2)
        leggInnOverføring("3", Trond, Kari, 15, 15, periode1)

        val overføringer = repository.hentAlleOverføringer(setOf(Ola)).medUtcTidssone()

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari, Trond)
        assertThat(overføringer.getValue(Ola).gitt).containsExactly(
            gitt("1", 10, 30, periode1, Kari)
        )
        assertThat(overføringer.getValue(Ola).fått).containsExactly(
            fått("2", 6, periode2, Trond)
        )
        assertThat(overføringer.getValue(Kari).fått).containsExactly(
            fått("1", 10, periode1, Ola)
        )
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
        assertThat(overføringer.getValue(Trond).gitt).containsExactly(
            gitt("2", 6, 13, periode2, Ola)
        )
        assertThat(overføringer.getValue(Trond).fått).isEmpty()
    }

    @Test
    fun `henter bare aktive koronaoverføringer`() {
        leggInnOverføring("1", Ola, Kari, 10, 30, periode1)
        leggInnOverføring("2", Ola, Trond, 15, 15, periode1, GjeldendeOverføring.Status.Deaktivert)
        leggInnOverføring("3", Trond, Ola, 0, 13, periode2, GjeldendeOverføring.Status.Avslått)

        val overføringer = repository.hentAlleOverføringer(setOf(Ola)).medUtcTidssone()

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari)
        assertThat(overføringer.getValue(Ola).gitt).containsExactly(
            gitt("1", 10, 30, periode1, Kari)
        )
        assertThat(overføringer.getValue(Ola).fått).isEmpty()
        assertThat(overføringer.getValue(Kari).fått).containsExactly(
            fått("1", 10, periode1, Ola)
        )
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
    }

    @Test
    fun `returnerer tomt resultat uten treff`() {
        leggInnOverføring("1", Ola, Kari, 10, 30, periode1)

        assertThat(repository.hentAlleOverføringer(setOf(Trond))).isEmpty()
        assertThat(repository.hentAlleOverføringer(emptySet())).isEmpty()
    }

    private fun leggInnOverføring(
        behovssekvensId: String,
        fra: Saksnummer,
        til: Saksnummer,
        antallDager: Int,
        antallDagerØnsketOverført: Int,
        periode: Periode,
        status: GjeldendeOverføring.Status = GjeldendeOverføring.Status.Aktiv
    ) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                """
                    INSERT INTO koronaoverforing
                        (behovssekvens_id, gjennomfort, fom, tom, fra, til, antall_dager,
                         antall_dager_onsket_overfort, status, lovanvendelser)
                    VALUES
                        (:behovssekvens_id, :gjennomfort, :fom, :tom, :fra, :til, :antall_dager,
                         :antall_dager_onsket_overfort, :status, :lovanvendelser::jsonb)
                """.trimIndent(),
                mapOf(
                    "behovssekvens_id" to behovssekvensId,
                    "gjennomfort" to gjennomført.toOffsetDateTime(),
                    "fom" to periode.fom,
                    "tom" to periode.tom,
                    "fra" to fra,
                    "til" to til,
                    "antall_dager" to antallDager,
                    "antall_dager_onsket_overfort" to antallDagerØnsketOverført,
                    "status" to status.name,
                    "lovanvendelser" to lovanvendelser.somJson()
                )
            ).asUpdate)
        }
    }

    private companion object {
        private val gjennomført = ZonedDateTime.parse("2021-03-03T12:15:00Z")
        private val periode1 = Periode("2021-01-01/2021-12-31")
        private val periode2 = Periode("2021-03-03/2021-12-31")
        private const val Ola = "Ola"
        private const val Kari = "Kari"
        private const val Trond = "Trond"

        private fun Map<Saksnummer, GjeldendeOverføringer>.medUtcTidssone() = mapValues { (_, overføringer) ->
            GjeldendeOverføringer(
                gitt = overføringer.gitt.map {
                    it.copy(gjennomført = it.gjennomført.withZoneSameInstant(ZoneOffset.UTC))
                },
                fått = overføringer.fått.map {
                    it.copy(gjennomført = it.gjennomført.withZoneSameInstant(ZoneOffset.UTC))
                }
            )
        }

        private fun fått(
            behovssekvensId: String,
            antallDager: Int,
            periode: Periode,
            fra: Saksnummer
        ) = GjeldendeOverføringFått(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            fra = fra,
            gjennomført = gjennomført,
            kilder = setOf(Kilde.internKilde(behovssekvensId, "KoronaOverføring")),
            lovanvendelser = lovanvendelser
        )

        private fun gitt(
            behovssekvensId: String,
            antallDager: Int,
            antallDagerØnsketOverført: Int,
            periode: Periode,
            til: Saksnummer
        ) = GjeldendeOverføringGitt(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            til = til,
            gjennomført = gjennomført,
            kilder = setOf(Kilde.internKilde(behovssekvensId, "KoronaOverføring")),
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = antallDagerØnsketOverført
        )

        private val lovanvendelser = Lovanvendelser()
            .leggTil(
                periode = periode1,
                lovhenvisning = JobberINorge,
                anvendelse = "Tester repository"
            )
    }
}
