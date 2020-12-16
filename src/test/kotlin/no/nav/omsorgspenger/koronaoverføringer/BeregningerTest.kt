package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal class BeregningerTest {

    @Test
    fun `Ingen barn`() {
        val behovet = behovet(
            omsorgsdagerTattUtIÅr = 0,
        )
        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = Behandling(
                overføringen = behovet
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
                overføringen = behovet
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
                overføringen = behovet
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
                overføringen = behovet
            ),
            grunnlag = grunnlag(
                behovet = behovet
            )
        )
        assertEquals(0, dagerTilgjengeligForOverføring)
    }

    private companion object {
        private fun grunnlag(
            behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
            overføringer: List<SpleisetOverføringGitt> = emptyList(),
            fordelinger: List<FordelingGirMelding> = emptyList(),
            koronaoverføringer: List<GjeldendeOverføringGitt> = emptyList(),
            utvidetRett: List<UtvidetRettVedtak> = emptyList()
        ) = Grunnlag(
            overføringen = behovet,
            overføringer = overføringer,
            fordelinger = fordelinger,
            koronaoverføringer = koronaoverføringer,
            utvidetRett = utvidetRett)

        private fun behovet(
            omsorgsdagerTattUtIÅr: Int,
            barn: List<OverføreKoronaOmsorgsdagerMelding.Barn> = emptyList()
        ) = OverføreKoronaOmsorgsdagerMelding.Behovet(
            fra = IdentitetsnummerGenerator.identitetsnummer(),
            til = IdentitetsnummerGenerator.identitetsnummer(),
            jobberINorge = true,
            periode = Periode("2021-01-01/2021-12-31"),
            mottatt = ZonedDateTime.now(),
            mottaksdato = LocalDate.now(),
            journalpostIder = setOf("123"),
            omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
            omsorgsdagerÅOverføre = 10,
            barn = barn
        )

        private fun barn(
            fødselsdato: LocalDate = LocalDate.now().minusYears(5),
            aleneOmOmsorgen: Boolean = false,
            utvidetRett: Boolean = false
        ) = OverføreKoronaOmsorgsdagerMelding.Barn(
            identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
            fødselsdato = LocalDate.now().minusYears(5),
            aleneOmOmsorgen = aleneOmOmsorgen,
            utvidetRett = utvidetRett
        )
    }
}