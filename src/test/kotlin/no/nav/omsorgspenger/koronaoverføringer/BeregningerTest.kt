package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.barn
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.behovet
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.grunnlag
import org.junit.jupiter.api.Test
import java.time.Duration
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

    @Test
    fun `trekker fra fordelte dager`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 0,
            barn = listOf(barn())
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                behovet = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet,
                fordelinger = listOf(
                    FordelingGirMelding(
                        periode = behovet.periode,
                        lengde = Duration.ofDays(6),
                        kilder = emptySet()
                    ),
                    // Sjekker kun på overlapp med minst èn dag og tar den hvor man fordeler bort flest dager.
                    // Derfor skal det kun regnes med den over på 6 dager
                    FordelingGirMelding(
                        periode = Periode(
                            fom = behovet.periode.fom.plusMonths(1),
                            tom = behovet.periode.tom.minusMonths(1)
                        ),
                        lengde = Duration.ofDays(5),
                        kilder = emptySet()
                    )
                )
            )
        )
        assertEquals(14, dagerTilgjengeligForOverføring)
    }
}