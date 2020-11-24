package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal class OverordnetPeriodeTest {

    @Test
    fun `Et yngre barn uten alene om omsorgen`() {
        val behovet = overføreOmsorgsdager(
            barn = listOf(
                Barn(
                    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
                    fødselsdato = LocalDate.parse("2018-02-15"),
                    aleneOmOmsorgen = true,
                    utvidetRett = false
                ),
                Barn(
                    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
                    fødselsdato = LocalDate.parse("2020-03-20"),
                    aleneOmOmsorgen = false,
                    utvidetRett = false
                )
            )
        )
        assertEquals(Periode("2021-09-29/2030-12-31"), behovet.overordnetPeriode)
    }

    @Test
    fun `To barn med alene om omsorgen`() {
        val behovet = overføreOmsorgsdager(
            barn = listOf(
                Barn(
                    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
                    fødselsdato = LocalDate.parse("2018-02-15"),
                    aleneOmOmsorgen = true,
                    utvidetRett = false
                ),
                Barn(
                    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
                    fødselsdato = LocalDate.parse("2020-03-20"),
                    aleneOmOmsorgen = true,
                    utvidetRett = false
                )
            )
        )
        assertEquals(Periode("2021-09-29/2032-12-31"), behovet.overordnetPeriode)
    }

    private companion object {
        fun overføreOmsorgsdager(
            barn: List<Barn>
        ) = OverføreOmsorgsdagerMelding.Behovet(
            overførerFra = IdentitetsnummerGenerator.identitetsnummer(),
            overførerTil = IdentitetsnummerGenerator.identitetsnummer(),
            jobberINorge = true,
            sendtPerBrev = false,
            mottatt = ZonedDateTime.parse("2021-09-29T14:15:00.000Z"),
            journalpostIder = setOf(),
            relasjon = OverføreOmsorgsdagerMelding.Relasjon.NåværendeEktefelle,
            harBoddSammentMinstEttÅr = true,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 5,
            barn = barn
        )
    }
}