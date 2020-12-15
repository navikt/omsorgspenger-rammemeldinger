package no.nav.omsorgspenger.omsorgsdager

import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.beregnOmsorgsdager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OmsorgsdagerBeregningTest {

    @Test
    fun `Ingen barn gir 0 dager`() {
        val omsorgsdagerResultat = beregnOmsorgsdager(listOf())
        omsorgsdagerResultat.assertAntallOmsorgsdager(0)
    }

    @Test
    fun `1 til 2 barn gir 10 dager`() {
        val etBarn = Barn(
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        for (antallBarn in 1..2) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = 1.rangeTo(antallBarn).map { etBarn }
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(10)
        }
    }

    @Test
    fun `3 eller fler barn gir 15 dager`() {
        val etBarn = Barn(
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        for (antallBarn in 3..23 step 5) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = 1.rangeTo(antallBarn).map { etBarn }
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(15)
        }
    }

    @Test
    fun `1 til 2 barn med aleneomsorg for, gir 10 ekstra dager`() {
        val etBarn = Barn(
            aleneOmOmsorgen = true,
            utvidetRett = false,
        )


        for (antallBarn in 1..2) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = 1.rangeTo(antallBarn).map { etBarn }
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(20)
        }
    }

    @Test
    fun `3 eller fler barn med aleneomsorg for, gir 15 ekstra dager`() {
        val etBarn = Barn(
            aleneOmOmsorgen = true,
            utvidetRett = false,
        )

        for (antallBarn in 3..23 step 5) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = 1.rangeTo(antallBarn).map { etBarn }
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(30)
        }
    }

    @Test
    fun `Får 10 ekstra dager per barn med utvidet rett`() {
        val vanligBarn = Barn(
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val vanligeBarn = 1.rangeTo(3).map { vanligBarn }

        val utvidetRettBarn = Barn(
            aleneOmOmsorgen = false,
            utvidetRett = true,
        )

        for (antallUtvidetRett in 1..5) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = vanligeBarn.plus(1.rangeTo(antallUtvidetRett).map { utvidetRettBarn })
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(15 + (10 * antallUtvidetRett))
        }
    }

    @Test
    fun `Får 20 ekstra dager per barn med utvidet rett og aleneomsorg, i tillegg til vanlig aleneomsorgsdager`() {
        val aleneOgUtvidetRettBarn = Barn(
            aleneOmOmsorgen = true,
            utvidetRett = true,
        )

        for (antallUtvidetOgAlene in 1..2) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = 1.rangeTo(antallUtvidetOgAlene).map { aleneOgUtvidetRettBarn }
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(20 + (20 * antallUtvidetOgAlene))
        }

        val vanligBarn = Barn(
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val vanligeBarn = 1.rangeTo(3).map { vanligBarn }

        for (antallUtvidetOgAlene in 3..23 step 5) {
            val omsorgsdagerResultat = beregnOmsorgsdager(
                barnMedOmsorgenFor = vanligeBarn.plus(1.rangeTo(antallUtvidetOgAlene).map { aleneOgUtvidetRettBarn })
            )
            omsorgsdagerResultat.assertAntallOmsorgsdager(30 + (20 * antallUtvidetOgAlene))
        }
    }
    

    private companion object {

        private fun OmsorgsdagerResultat.assertAntallOmsorgsdager(forventetAntallOmsorgsdager: Int) {
            assertEquals(forventetAntallOmsorgsdager, antallOmsorgsdager)
            assertEquals(forventetAntallOmsorgsdager * 2, kopier(faktor = 2).antallOmsorgsdager)
        }

        private data class Barn(
            override val aleneOmOmsorgen: Boolean,
            override val utvidetRett: Boolean) : OmsorgsdagerBarn
    }
}