package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class OverføringRepositoryTest(
    dataSource: DataSource) {
    private val overføringRepository = OverføringRepository(
        dataSource = dataSource.cleanAndMigrate()
    )
    @Test
    fun `Håndtere overføringer gjennom samlivsbrudd`() {
        val År2020 = Periode("2020-01-01/2020-12-31")

        var gjennomførte = overføringRepository.gjennomførOverføringer(
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

        gjennomførte = overføringRepository.gjennomførOverføringer(
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
        assertEquals(emptySet(), overføringRepository.hentBerørteSaksnummer(
            fra = Trond,
            til = Ola,
            fraOgMed = År2020.tom.plusDays(1)
        ))

        val November2020 = Periode("2020-11-01/2020-11-30")
        gjennomførte = overføringRepository.gjennomførOverføringer(
            fra = Trond,
            til = Ola,
            overføringer = listOf(
                (November2020 to 7).somOverføring()
            )
        )

        assertThat(setOf(Ola, Kari, Trond)).hasSameElementsAs(gjennomførte.keys)
        assertThat(setOf(gitt(7, November2020, Ola))).hasSameElementsAs(gjennomførte.getValue(Trond).gitt)
        assertTrue(gjennomførte.getValue(Trond).fått.isEmpty())

        val nyPeriodeForOverføringMellomOlaOgKari = Periode(
            fom = År2020.fom,
            tom = November2020.fom.minusDays(1)
        )

        assertThat(setOf(
            gitt(1, nyPeriodeForOverføringMellomOlaOgKari, Kari)
        )).hasSameElementsAs(gjennomførte.getValue(Ola).gitt)
        assertThat(setOf(
            fått(5, nyPeriodeForOverføringMellomOlaOgKari, Kari),
            fått(7, November2020, Trond)
        )).hasSameElementsAs(gjennomførte.getValue(Ola).fått)

        assertThat(setOf(
            gitt(5, nyPeriodeForOverføringMellomOlaOgKari, Ola)
        )).hasSameElementsAs(gjennomførte.getValue(Kari).gitt)
        assertThat(setOf(
            fått(1, nyPeriodeForOverføringMellomOlaOgKari, Ola),
        )).hasSameElementsAs(gjennomførte.getValue(Kari).fått)
    }

    private companion object {
        private const val Ola = "Ola"
        private const val Kari = "Kari"
        private const val Trond = "Trond"
        private const val Hege = "Hege"

        private fun Pair<Periode, Int>.somOverføring() = NyOverføring(
            antallDager = second,
            periode = first,
            starterGrunnet = listOf(),
            slutterGrunnet = listOf()
        )

        private fun fått(antallDager: Int, periode: Periode, fra: Saksnummer) = GjeldendeOverføringFått(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            fra = fra
        )

        private fun gitt(antallDager: Int, periode: Periode, til: Saksnummer) = GjeldendeOverføringGitt(
            antallDager = antallDager,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            til = til
        )
    }
}