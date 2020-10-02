package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.medAlleRivers
import no.nav.omsorgspenger.overføringer.IdentitetsnummerGenerator.identitetsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class IkkeVerifiserbareUtvidetRettRammevedtakTest {

    private val rapid = TestRapid().apply {
        medAlleRivers()
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Utvidet rett for barnet kan ikke verifiseres som gjør at ikke ønsket antall dager kan overføres`() {
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val omsorgsdagerÅOverføre = 5

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 16,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            barn = listOf(overføreOmsorgsdagerBarn(
                utvidetRett = true,
                aleneOmOmsorgen = true
            ))
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.ikkeBehandlesAvNyttSystem())
        assertTrue(løsning.overføringer.isEmpty())
    }

    @Test
    fun `Utvidet rett for barnet kan ikke verifiseres men er alikevel nok dager tilgjengelig`() {
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val omsorgsdagerÅOverføre = 5
        val barnetsFødselsdato = LocalDate.parse("2019-09-29")
        val mottaksdato = LocalDate.parse("2020-01-15")

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 5,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            mottaksdato = mottaksdato,
            barn = listOf(overføreOmsorgsdagerBarn(
                utvidetRett = true,
                aleneOmOmsorgen = true,
                fødselsdato = barnetsFødselsdato
            ))
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåHentePersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(antallMeldinger = 2)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2020-01-15/2031-12-31") to 5 // Merk at overføringen da kun gjelder ut barnet er 12 år, ikke 18.
            )
        )
    }
}