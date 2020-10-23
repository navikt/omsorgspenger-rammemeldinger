package no.nav.omsorgspenger.overføringer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BeregningerTest {

    @Test
    internal fun `1 til 2 barn gir 10 dager`() {
        for (antallBarn in 1..2) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = antallBarn,
                antallBarnMedAleneOmOmsorgen = 0,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = 0
            )

            assertEquals(omsorgsdager, 10)
        }
    }

    @Test
    internal fun `3 eller fler barn gir 15 dager`() {
        for (antallBarn in 3..23 step 5) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = antallBarn,
                antallBarnMedAleneOmOmsorgen = 0,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = 0
            )

            assertEquals(omsorgsdager, 15)
        }
    }

    @Test
    internal fun `1 til 2 barn med aleneomsorg for, gir 10 ekstra dager`() {
        for (antallBarn in 1..2) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = 2,
                antallBarnMedAleneOmOmsorgen = antallBarn,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = 0
            )

            assertEquals(omsorgsdager, 20)
        }
    }

    @Test
    internal fun `3 eller fler barn med aleneomsorg for, gir 15 ekstra dager`() {
        for (antallBarn in 3..23 step 5) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = antallBarn,
                antallBarnMedAleneOmOmsorgen = antallBarn,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = 0
            )

            assertEquals(omsorgsdager, 30)
        }
    }

    @Test
    internal fun `Får 10 ekstra dager per barn med utvidet rett`() {
        for (antallUtvidetRett in 1..5) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = 5,
                antallBarnMedAleneOmOmsorgen = 0,
                antallBarnMedUtvidetRett = antallUtvidetRett,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = 0
            )

            assertEquals(omsorgsdager, 15 + (10 * antallUtvidetRett))
        }
    }

    @Test
    internal fun `Får 20 ekstra dager per barn med utvidet rett og aleneomsorg, i tillegg til vanlig aleneomsorgsdager`() {
        for (antallUtvidetOgAlene in 0..2) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = 2,
                antallBarnMedAleneOmOmsorgen = 2,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = antallUtvidetOgAlene
            )

            assertEquals(omsorgsdager, 20 + (20 * antallUtvidetOgAlene))
        }

        for (antallUtvidetOgAlene in 3..23) {
            val omsorgsdager = Beregninger.beregnOmsorgsdager(
                antallBarnMedOmsorgenFor = 30,
                antallBarnMedAleneOmOmsorgen = 15,
                antallBarnMedUtvidetRett = 0,
                antallBarnMedUtvidetRettOgAleneOmOmsorgen = antallUtvidetOgAlene
            )

            assertEquals(omsorgsdager, 30 + (20 * antallUtvidetOgAlene))
        }
    }
}
