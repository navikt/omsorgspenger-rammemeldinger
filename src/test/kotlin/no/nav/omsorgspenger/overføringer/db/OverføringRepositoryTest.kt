package no.nav.omsorgspenger.overføringer.db

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.JobberINorge
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class OverføringRepositoryTest(
    dataSource: DataSource) {
    private val overføringRepository = OverføringRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `Håndtere overføringer gjennom samlivsbrudd`() {
        val År2020 = Periode("2020-01-01/2020-12-31")

        var gjennomførte = gjennomførOverføringer(
            fra = Ola,
            til = Kari,
            overføringer = listOf(
                (År2020 to 1).somOverføring()
            )
        )
        assertThat(setOf(Ola, Kari)).hasSameElementsAs(gjennomførte.keys)

        assertTrue(gjennomførte.getValue(Kari).gitt.isEmpty())
        assertThat(setOf(fått(1, År2020, Ola))).hasSameElementsAs(gjennomførte.getValue(Kari).fått)

        assertThat(setOf(gitt(1, År2020, Kari))).hasSameElementsAs(gjennomførte.getValue(Ola).gitt)
        assertTrue(gjennomførte.getValue(Ola).fått.isEmpty())

        gjennomførte = gjennomførOverføringer(
            fra = Kari,
            til = Ola,
            overføringer = listOf(
                (År2020 to 5).somOverføring()
            )
        )

        assertThat(setOf(Ola, Kari)).hasSameElementsAs(gjennomførte.keys)

        assertThat(setOf(gitt(5, År2020, Ola))).hasSameElementsAs(gjennomførte.getValue(Kari).gitt)
        assertThat(setOf(fått(1, År2020, Ola))).hasSameElementsAs(gjennomførte.getValue(Kari).fått)

        assertThat(setOf(gitt(1, År2020, Kari))).hasSameElementsAs(gjennomførte.getValue(Ola).gitt)
        assertThat(setOf(fått(5, År2020, Kari))).hasSameElementsAs(gjennomførte.getValue(Ola).fått)

        assertEquals(setOf(Kari), overføringRepository.hentBerørteSaksnummer(
            fra = Trond,
            til = Ola,
            fraOgMed = År2020.tom
        ))
        assertEquals(emptySet<Saksnummer>(), overføringRepository.hentBerørteSaksnummer(
            fra = Trond,
            til = Ola,
            fraOgMed = År2020.tom.plusDays(1)
        ))

        val November2020 = Periode("2020-11-01/2020-11-30")
        gjennomførOverføringer(
            fra = Trond,
            til = Ola,
            overføringer = listOf(
                (November2020 to 7).somOverføring()
            )
        )

        gjennomførte = hentAktive(setOf(Ola, Kari, Trond))

        assertThat(setOf(Ola, Kari, Trond)).hasSameElementsAs(gjennomførte.keys)
        assertThat(setOf(
            gitt(7, November2020, Ola, setOf(
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser)
        )).hasSameElementsAs(gjennomførte.getValue(Trond).gitt)
        assertTrue(gjennomførte.getValue(Trond).fått.isEmpty())

        val nyPeriodeForOverføringMellomOlaOgKari = Periode(
            fom = År2020.fom,
            tom = November2020.fom.minusDays(1)
        )

        assertThat(setOf(
            gitt(1, nyPeriodeForOverføringMellomOlaOgKari, Kari, setOf(
                Kilde.internKilde("1", "Overføring"),
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser)
        )).hasSameElementsAs(gjennomførte.getValue(Ola).gitt)

        assertThat(setOf(
            fått(5, nyPeriodeForOverføringMellomOlaOgKari, Kari, setOf(
                Kilde.internKilde("2", "Overføring"),
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser),
                fått(7, November2020, Trond, setOf(
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser)
        )).hasSameElementsAs(gjennomførte.getValue(Ola).fått)


        assertThat(setOf(
            gitt(5, nyPeriodeForOverføringMellomOlaOgKari, Ola, setOf(
                Kilde.internKilde("2", "Overføring"),
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser)
        )).hasSameElementsAs(gjennomførte.getValue(Kari).gitt)
        assertThat(setOf(
            fått(1, nyPeriodeForOverføringMellomOlaOgKari, Ola, setOf(
                Kilde.internKilde("1", "Overføring"),
                Kilde.internKilde("3", "Overføring")
            ), lovanvendelser),
        )).hasSameElementsAs(gjennomførte.getValue(Kari).fått)
    }
    /*
        oveføringId : melding
        ---------------------------
        - 1 : Opprettet
        - 2 : Opprettet
        - 1 : Endret til og med til 2020-10-31
        - 2 : Endret til og med til 2020-10-31
        - 3 : Opprettet
     */


    @Test
    fun `Tidligere partner som får overføring deaktivert inngår også i gjeldende overføringer`() {
        val Erik = "Erik"
        val Silje = "Silje"
        val Ida = "Ida"
        val Periode1 = Periode("2020-11-01/2020-11-30")
        val Periode2 = Periode("2020-11-01/2020-12-31")

        var gjeldendeOverføringer = gjennomførOverføringer(
            fra = Erik,
            til = Silje,
            overføringer = listOf(
                (Periode1 to 5).somOverføring()
            )
        )

        assertThat(gjeldendeOverføringer.keys).hasSameElementsAs(setOf(Erik, Silje))

        gjeldendeOverføringer = gjennomførOverføringer(
            fra = Erik,
            til = Ida,
            overføringer = listOf(
                (Periode2 to 7).somOverføring()
            )
        )

        assertThat(gjeldendeOverføringer.keys).hasSameElementsAs(setOf(Erik, Silje, Ida))
        assertThat(gjeldendeOverføringer.getValue(Erik).gitt).hasSize(1)
        assertThat(gjeldendeOverføringer.getValue(Erik).fått).hasSize(0)
        assertThat(gjeldendeOverføringer.getValue(Ida).gitt).hasSize(0)
        assertThat(gjeldendeOverføringer.getValue(Ida).fått).hasSize(1)
        assertThat(gjeldendeOverføringer.getValue(Silje).fått).hasSize(0)
        assertThat(gjeldendeOverføringer.getValue(Silje).fått).hasSize(0)
    }

    private var behovsekvensCounter = 1
    private fun gjennomførOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<NyOverføring>) =
        overføringRepository.gjennomførOverføringer(
            behovssekvensId = "${behovsekvensCounter++}",
            fra = fra,
            til = til,
            overføringer = overføringer,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = overføringer.maxByOrNull { it.antallDager }!!.antallDager
        ).gjeldendeOverføringer.mapValues { (_, gjeldendeOverføringer) ->
            GjeldendeOverføringer(
                fått = gjeldendeOverføringer.fått.map { it.copy(gjennomført = Now) }.also { fått ->
                    require(fått.all { it.kilder.isEmpty()})
                },
                gitt = gjeldendeOverføringer.gitt.map { it.copy(gjennomført = Now) }.also { gitt ->
                    require(gitt.all { it.kilder.isEmpty() })
                }
            )
        }

    private fun hentAktive(
        saksnummer: Set<Saksnummer>
    ) = overføringRepository.hentAktiveOverføringer(
        saksnummer = saksnummer
    ).mapValues { (_, gjeldendeOverføringer) ->
        GjeldendeOverføringer(
            fått = gjeldendeOverføringer.fått.map { it.copy(gjennomført = Now) },
            gitt = gjeldendeOverføringer.gitt.map { it.copy(gjennomført = Now) }
        )
    }

    private companion object {
        private val Now = ZonedDateTime.now()
        private const val Ola = "Ola"
        private const val Kari = "Kari"
        private const val Trond = "Trond"
        private val lovanvendelser = Lovanvendelser()
            .leggTil(
                periode = Periode("2020-11-01/2020-11-30"),
                lovhenvisning = JobberINorge,
                anvendelse = "Tester repository"
            )
        
        private fun Pair<Periode, Int>.somOverføring() = NyOverføring(
            antallDager = second,
            periode = first,
            starterGrunnet = listOf(),
            slutterGrunnet = listOf()
        )

        private fun fått(
            antallDager: Int,
            periode: Periode,
            fra: Saksnummer,
            kilder: Set<Kilde> = setOf(),
            lovanvendelser: Lovanvendelser? = null) = GjeldendeOverføringFått(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            fra = fra,
            gjennomført = Now,
            kilder = kilder,
            lovanvendelser = lovanvendelser
        )

        private fun gitt(
            antallDager: Int,
            periode: Periode,
            til: Saksnummer,
            kilder: Set<Kilde> = setOf(),
            lovanvendelser: Lovanvendelser? = null) = GjeldendeOverføringGitt(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            til = til,
            gjennomført = Now,
            kilder = kilder,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = antallDager
        )
    }
}