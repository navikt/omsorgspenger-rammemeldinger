package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodisertOmsorsgdagerTilgjengeligTilOverføringerTest {

    @Test
    fun `Alle periodene har samme antall dager tilgjengelig for overføring`() {
        val forventedeOverføringer = listOf(Overføring(periode = Periode("2020-05-01/2040-01-01"), antallDager = 10, starterGrunnet = listOf(), slutterGrunnet = listOf()))
        val overføringer = dagerTilgjengeligPerPeriode(10,10,10,10).knekt().somOverføringer(10)
        assertEquals(forventedeOverføringer, overføringer)
    }

    @Test
    fun `Første periode har annet antall dager enn resterende perioder`() {
        val forventedeOverføringer = listOf(
            Overføring(periode = Periode("2020-05-01/2020-12-31"), antallDager = 5, starterGrunnet = listOf(), slutterGrunnet = listOf()),
            Overføring(periode = Periode("2021-01-01/2040-01-01"), antallDager = 10, starterGrunnet = listOf(), slutterGrunnet = listOf())
        )
        val overføringer = dagerTilgjengeligPerPeriode(5,10,10,10).knekt().somOverføringer(10)
        assertEquals(forventedeOverføringer, overføringer)
    }

    @Test
    fun `Perioder med hull i`() {
        val forventedeOverføringer = listOf(
            Overføring(periode = Periode("2020-05-01/2021-01-02"), antallDager = 10, starterGrunnet = listOf(), slutterGrunnet = listOf()),
            Overføring(periode = Periode("2021-01-10/2021-06-10"), antallDager = 5, starterGrunnet = listOf(), slutterGrunnet = listOf()),
            Overføring(periode = Periode("2021-07-01/2026-01-01"), antallDager = 10, starterGrunnet = listOf(), slutterGrunnet = listOf())
        )

        val perioder = mapOf(
            Periode("2020-05-01/2020-12-31") to 10,
            Periode("2021-01-01/2021-01-02") to 10,
            Periode("2021-01-10/2021-06-10") to 5,
            Periode("2021-07-01/2026-01-01") to 10
        )
        val overføringer = perioder.knekt().somOverføringer(10)

        assertEquals(forventedeOverføringer, overføringer)
    }

    private companion object {
        private fun dagerTilgjengeligPerPeriode(p1: Int, p2: Int, p3:Int, p4:Int) = mapOf(
            Periode("2020-05-01/2020-12-31") to p1,
            Periode("2021-01-01/2021-06-30") to p2,
            Periode("2021-07-01/2026-01-01") to p3,
            Periode("2026-01-02/2040-01-01") to p4,
        )
        private fun Map<Periode, Int>.knekt() = mapKeys { KnektPeriode(
            periode = it.key,
            starterGrunnet = listOf(),
            slutterGrunnet = listOf()
        )}
    }
}