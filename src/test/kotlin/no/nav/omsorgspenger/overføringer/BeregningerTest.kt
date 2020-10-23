package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BeregningerTest {

    @Test
    internal fun `1 til 2 barn gir 10 dager`() {
        val nå = LocalDate.now()
        val etBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val periode = Periode(nå, nå)

        for (antallBarn in 1..2) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = 1.rangeTo(antallBarn).map { etBarn },
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 10)
        }
    }

    @Test
    internal fun `3 eller fler barn gir 15 dager`() {
        val nå = LocalDate.now()
        val etBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val periode = Periode(nå, nå)

        for (antallBarn in 3..23 step 5) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = 1.rangeTo(antallBarn).map { etBarn },
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 15)
        }
    }

    @Test
    internal fun `1 til 2 barn med aleneomsorg for, gir 10 ekstra dager`() {
        val nå = LocalDate.now()
        val etBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = true,
            utvidetRett = false,
        )

        val periode = Periode(nå, nå)

        for (antallBarn in 1..2) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = 1.rangeTo(antallBarn).map { etBarn },
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 20)
        }
    }

    @Test
    internal fun `3 eller fler barn med aleneomsorg for, gir 15 ekstra dager`() {
        val nå = LocalDate.now()
        val etBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = true,
            utvidetRett = false,
        )

        val periode = Periode(nå, nå)

        for (antallBarn in 3..23 step 5) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = 1.rangeTo(antallBarn).map { etBarn },
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 30)
        }
    }

    @Test
    internal fun `Får 10 ekstra dager per barn med utvidet rett`() {
        val nå = LocalDate.now()
        val vanligBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val vanligeBarn = 1.rangeTo(3).map { vanligBarn }

        val utvidetRettBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = false,
            utvidetRett = true,
        )

        val periode = Periode(nå, nå)

        for (antallUtvidetRett in 1..5) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = vanligeBarn.plus(1.rangeTo(antallUtvidetRett).map { utvidetRettBarn }),
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 15 + (10 * antallUtvidetRett))
        }
    }

    @Test
    internal fun `Får 20 ekstra dager per barn med utvidet rett og aleneomsorg, i tillegg til vanlig aleneomsorgsdager`() {
        val nå = LocalDate.now()
        val aleneOgUtvidetRettBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = true,
            utvidetRett = true,
        )

        val periode = Periode(nå, nå)

        for (antallUtvidetOgAlene in 1..2) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = 1.rangeTo(antallUtvidetOgAlene).map { aleneOgUtvidetRettBarn },
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 20 + (20 * antallUtvidetOgAlene))
        }

        val vanligBarn = Barn(
            identitetsnummer = "",
            fødselsdato = nå.minusYears(1),
            aleneOmOmsorgen = false,
            utvidetRett = false,
        )

        val vanligeBarn = 1.rangeTo(3).map { vanligBarn }

        for (antallUtvidetOgAlene in 3..23 step 5) {
            val omsorgsdagerResultat = Beregninger.beregnOmsorgsdager(
                barn = vanligeBarn.plus(1.rangeTo(antallUtvidetOgAlene).map { aleneOgUtvidetRettBarn }),
                periode = periode
            )

            assertEquals(omsorgsdagerResultat.antallOmsorgsdager(), 30 + (20 * antallUtvidetOgAlene))
        }
    }
}
