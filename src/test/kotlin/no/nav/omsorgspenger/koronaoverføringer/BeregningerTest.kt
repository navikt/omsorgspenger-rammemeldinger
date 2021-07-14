package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.barn
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.behovet
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.grunnlag
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.overføring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

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
                        periode = Periode("2021-02-05/2022-05-05"),
                        lengde = Duration.ofDays(5),
                        kilder = emptySet()
                    )
                )
            )
        )
        assertEquals(14, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `trekker fra overførte dager`() {
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
                overføringer = listOf(
                    overføring(periode = Periode("2020-01-01/2020-12-31"), antallDager = 10), // Er før perioden
                    overføring(periode = behovet.periode, antallDager = 6), // Regnes ikke med siden det også finnes en på 7
                    overføring(periode = Periode("2021-02-01/2025-04-10"), antallDager = 7) // Skal trekkes fra
                )
            )
        )
        assertEquals(13, dagerTilgjengeligForOverføring)
    }

    @Test
    fun `trekker fra koronaoverførte dager`() {
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
                koronaoverføringer = listOf(
                    overføring(periode = Periode("2020-01-01/2020-12-31"), antallDager = 10), // Er før perioden
                    overføring(periode = behovet.periode, antallDager = 6),
                    overføring(periode = Periode("2021-02-01/2025-04-10"), antallDager = 4) // koronaoveføringer summeres så skal bli 6+4
                )
            )
        )
        assertEquals(10, dagerTilgjengeligForOverføring)
    }
}