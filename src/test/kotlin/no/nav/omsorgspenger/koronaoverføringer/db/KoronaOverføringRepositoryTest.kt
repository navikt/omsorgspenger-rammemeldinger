package no.nav.omsorgspenger.koronaoverføringer.db

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
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
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class KoronaOverføringRepositoryTest(
    dataSource: DataSource) {
    private val repository = KoronaoverføringRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `lagre og hente koronaoverføringer`() {
        val periode1 = Periode("2021-01-01/2021-12-31")
        // Ola -> Kari 10 dager.
        var gjennomførteOverføringer = repository.gjennomførOverføringer(
            behovssekvensId = "1",
            fra = Ola,
            til = Kari,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = 30,
            overføringer = listOf(NyOverføring(
                antallDager = 10,
                periode = periode1
            ))
        )
        assertThat(gjennomførteOverføringer.berørteSaksnummer).isEqualTo(setOf(Ola, Kari))
        assertThat(gjennomførteOverføringer.alleSaksnummer).isEqualTo(setOf(Ola, Kari))
        assertThat(gjennomførteOverføringer.gjeldendeOverføringer.keys).isEqualTo(setOf(Ola, Kari))
        var gjeldendeOverføringer = gjennomførteOverføringer.gjeldendeOverføringer.overstyrGjennomført()
        assertThat(setOf(gitt(
            behovssekvensId = "1",
            antallDager = 10,
            antallDagerØnsketOverført = 30,
            til = Kari,
            periode = periode1

        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Ola).gitt)
        assertThat(gjeldendeOverføringer.getValue(Ola).fått).isEmpty()
        assertThat(setOf(fått(
            behovssekvensId = "1",
            antallDager = 10,
            periode = periode1,
            fra = Ola
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Kari).fått)
        assertThat(gjeldendeOverføringer.getValue(Kari).gitt).isEmpty()
        // Ola -> Trond: 15 dager
        gjennomførteOverføringer = repository.gjennomførOverføringer(
            behovssekvensId = "2",
            fra = Ola,
            til = Trond,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = 15,
            overføringer = listOf(NyOverføring(
                antallDager = 15,
                periode = periode1
            ))
        )
        assertThat(gjennomførteOverføringer.berørteSaksnummer).isEqualTo(setOf(Ola, Trond))
        assertThat(gjennomførteOverføringer.alleSaksnummer).isEqualTo(setOf(Ola, Kari, Trond))
        assertThat(gjennomførteOverføringer.gjeldendeOverføringer.keys).isEqualTo(setOf(Ola, Kari, Trond))
        gjeldendeOverføringer = gjennomførteOverføringer.gjeldendeOverføringer.overstyrGjennomført()
        assertThat(setOf(gitt(
            behovssekvensId = "1",
            antallDager = 10,
            antallDagerØnsketOverført = 30,
            til = Kari,
            periode = periode1
        ), gitt(
            behovssekvensId = "2",
            antallDager = 15,
            antallDagerØnsketOverført = 15,
            til = Trond,
            periode = periode1
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Ola).gitt)
        assertThat(gjeldendeOverføringer.getValue(Ola).fått).isEmpty()
        assertThat(setOf(fått(
            behovssekvensId = "1",
            antallDager = 10,
            periode = periode1,
            fra = Ola
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Kari).fått)
        assertThat(gjeldendeOverføringer.getValue(Kari).gitt).isEmpty()
        assertThat(setOf(fått(
            behovssekvensId = "2",
            antallDager = 15,
            periode = periode1,
            fra = Ola
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Trond).fått)
        assertThat(gjeldendeOverføringer.getValue(Trond).gitt).isEmpty()
        // Trond -> Kari: 6 dager
        val periode2 = Periode("2021-03-03/2021-12-31")
        gjennomførteOverføringer = repository.gjennomførOverføringer(
            behovssekvensId = "3",
            fra = Trond,
            til = Kari,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = 13,
            overføringer = listOf(NyOverføring(
                antallDager = 6,
                periode = periode2
            ))
        )
        assertThat(gjennomførteOverføringer.berørteSaksnummer).isEqualTo(setOf(Trond, Kari))
        assertThat(gjennomførteOverføringer.alleSaksnummer).isEqualTo(setOf(Ola, Kari, Trond))
        assertThat(gjennomførteOverføringer.gjeldendeOverføringer.keys).isEqualTo(setOf(Ola, Kari, Trond))
        gjeldendeOverføringer = gjennomførteOverføringer.gjeldendeOverføringer.overstyrGjennomført()
        assertThat(setOf(gitt(
            behovssekvensId = "1",
            antallDager = 10,
            antallDagerØnsketOverført = 30,
            til = Kari,
            periode = periode1
        ), gitt(
            behovssekvensId = "2",
            antallDager = 15,
            antallDagerØnsketOverført = 15,
            til = Trond,
            periode = periode1
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Ola).gitt)
        assertThat(gjeldendeOverføringer.getValue(Ola).fått).isEmpty()
        assertThat(setOf(fått(
            behovssekvensId = "1",
            antallDager = 10,
            periode = periode1,
            fra = Ola
        ), fått(
            behovssekvensId = "3",
            antallDager = 6,
            periode = periode2,
            fra = Trond
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Kari).fått)
        assertThat(gjeldendeOverføringer.getValue(Kari).gitt).isEmpty()
        assertThat(setOf(fått(
            behovssekvensId = "2",
            antallDager = 15,
            periode = periode1,
            fra = Ola
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Trond).fått)
        assertThat(setOf(gitt(
            behovssekvensId = "3",
            antallDager = 6,
            antallDagerØnsketOverført = 13,
            periode = periode2,
            til = Kari
        ))).hasSameElementsAs(gjeldendeOverføringer.getValue(Trond).gitt)
    }

    private companion object {
        private val Now = ZonedDateTime.now()
        private const val Ola = "Ola"
        private const val Kari = "Kari"
        private const val Trond = "Trond"

        private fun Map<Saksnummer,GjeldendeOverføringer>.overstyrGjennomført() = mapValues { (_, gjeldendeOverføringer) ->
            GjeldendeOverføringer(
                fått = gjeldendeOverføringer.fått.map { it.copy(gjennomført = Now) },
                gitt = gjeldendeOverføringer.gitt.map { it.copy(gjennomført = Now) }
            )
        }

        private fun fått(
            antallDager: Int,
            periode: Periode,
            fra: Saksnummer,
            behovssekvensId: BehovssekvensId) = GjeldendeOverføringFått(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            fra = fra,
            gjennomført = Now,
            kilder = setOf(Kilde.internKilde(
                behovssekvensId = behovssekvensId,
                type = "KoronaOverføring"
            )),
            lovanvendelser = lovanvendelser
        )

        private fun gitt(
            behovssekvensId: BehovssekvensId,
            antallDager: Int,
            antallDagerØnsketOverført: Int = antallDager,
            periode: Periode,
            til: Saksnummer) = GjeldendeOverføringGitt(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            til = til,
            gjennomført = Now,
            kilder = setOf(Kilde.internKilde(
                behovssekvensId = behovssekvensId,
                type = "KoronaOverføring"
            )),
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = antallDagerØnsketOverført
        )

        private val lovanvendelser = Lovanvendelser()
            .leggTil(
                periode = Periode("2021-01-01/2021-12-31"),
                lovhenvisning = JobberINorge,
                anvendelse = "Tester repository"
            )
    }
}