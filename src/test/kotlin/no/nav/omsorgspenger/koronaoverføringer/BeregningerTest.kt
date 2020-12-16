package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.barn
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.behovet
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.grunnlag
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

internal class BeregningerTest {

    @Test
    fun `Ingen barn`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 0,
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(0, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `Ett barn`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 0,
            barn = listOf(barn())
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(20, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `2 barn og brukt noen dager i år`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 10,
            barn = listOf(barn(), barn())
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(10, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `2 barn og brukt alle dager i år`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 50,
            barn = listOf(barn(), barn())
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(0, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `barn utenfor omsorgen for telles ikke med`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 0,
            barn = listOf(
                barn(fødselsdato = LocalDate.now().minusYears(13)),
                barn(fødselsdato = LocalDate.now().minusYears(19), utvidetRett = true)
            )
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(0, dagerTilgjengeligForOverføring)
    }
}