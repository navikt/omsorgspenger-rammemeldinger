package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBarn
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerResultat
import no.nav.omsorgspenger.overføringer.Beregninger.antallKoronaoverførteDagerSomSkalTrekkesFraTilgjengelig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AntallKoronaoverførteDagerSomSkalTrekkesFraTilgjengeligTest {

    @Test
    fun `Samme antall omsorgsdager i grunnlaget ved begge overføringene`() {
        // Koronaoverført 0, ingenting skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 0, koronaoverført = 0)
        // Koronaoverført 10, kun brukt halvparten av ekstradager, ingenting skal trekkfes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 0, koronaoverført = 10)
        // Koronaoverført 20 -> Kun brukt ekstradager, ingenting skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 0, koronaoverført = 20)
        // Koronaoverført 25 -> Brukt ekstradager + 5 grunnrett, ingenting skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 0, koronaoverført = 25)
        // Koronaoverført 30 -> Brukt ekstradager + hele grunnrett, ingenting skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 0, koronaoverført = 30)
        // Koronaoverført 35 -> Brukt ekstradager + hele grunnrett + 5 av de ordinære dagene, sistnevnte skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 5, koronaoverført = 35)
        // Koronaoverført 40 -> Brukt ekstradager + hele grunnrett + 10 ordinære dagene, sistnevnte skal trekkes fra
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 10, koronaoverført = 40)
    }

    @Test
    fun `Fler omsorgsdager i grunnlaget ved koronaoverføringen i forhold til den ordinære overføringen`() {
        // Har hatt et grunnlag med fler tilgjengelige dager ved koronaoverføringen
        // Koronaoveført 60 -> Brukt ekstradager + hele grunnrett + 10 ordinære dagene + 20 dager vi ikke ser i dette grunnlaget
        omsorgsdagerResultat2Barn.assertTrekkesFra(forventet = 30, koronaoverført = 60)
    }

    @Test
    fun `Fler omsorgsdager i grunnlaget ved den ordinære overføringen i forhold til koronaoverføringen`() {
        // Koronaoverføring med 2 barn i grunnlaget, kan maks overføre 40 dager
        assertEquals(40, omsorgsdagerResultat2Barn.koronaKopi().antallOmsorgsdager)

        // Ordinær overføring med 3 barn i grunnlaget, kunne ved en koronaoverføring ha overført 50 dager
        assertEquals(50, omsorgsdagerResultat3Barn.koronaKopi().antallOmsorgsdager)

         // Koronaoverført 40 -> Trekker ikke fra noe
        omsorgsdagerResultat3Barn.assertTrekkesFra(forventet = 0, koronaoverført = 40)
        // Koronaoverført 45 -> Trekker fra 5
        omsorgsdagerResultat3Barn.assertTrekkesFra(forventet = 5, koronaoverført = 45)
    }

    private companion object {
        val barn1 = Barn(aleneOmOmsorgen = true)
        val barn2 = Barn()
        val barn3 = Barn()

        val omsorgsdagerResultat2Barn = OmsorgsdagerBeregning.beregnOmsorgsdager(
            listOf(barn1, barn2)
        ).also {
            assertEquals(20, it.antallOmsorgsdager)
        }

        val omsorgsdagerResultat3Barn = OmsorgsdagerBeregning.beregnOmsorgsdager(
            listOf(barn1, barn2, barn3)
        ).also {
            assertEquals(25, it.antallOmsorgsdager)
        }

        private data class Barn(
            override val aleneOmOmsorgen: Boolean = false,
            override val utvidetRett: Boolean = false) : OmsorgsdagerBarn

        private fun OmsorgsdagerResultat.koronaKopi() = kopier(2)

        private fun OmsorgsdagerResultat.assertTrekkesFra(forventet: Int, koronaoverført: Int) =
            assertEquals(forventet, antallKoronaoverførteDagerSomSkalTrekkesFraTilgjengelig(
                koronaoverførteDager = koronaoverført,
                omsorgsdagerResultat = this
            ))
    }
}


