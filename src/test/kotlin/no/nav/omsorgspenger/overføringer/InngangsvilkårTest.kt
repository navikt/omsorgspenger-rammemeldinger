package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.omsorgspenger.medAlleRivers
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InngangsvilkårTest {

    private val rapid = TestRapid().apply {
        medAlleRivers()
    }

    private val fra = IdentitetsnummerGenerator.identitetsnummer()
    private val til = IdentitetsnummerGenerator.identitetsnummer()

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Jobber ikke i Norge`() {
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            jobberINorge = false,
            barn = listOf(
                overføreOmsorgsdagerBarn(
                    aleneOmOmsorgen = true
                )
            )
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåPersonopplysningerOgSaksnummer(
            fra = fra,
            til = til
        )
        rapid.ventPå(antallMeldinger = 2)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }

    @Test
    fun `Bor ikke i Norge`() {
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            borINorge = false,
            barn = listOf(
                overføreOmsorgsdagerBarn(
                    aleneOmOmsorgen = true
                )
            )
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåPersonopplysningerOgSaksnummer(
            fra = fra,
            til = til
        )
        rapid.ventPå(antallMeldinger = 2)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }

    @Test
    fun `Ikke bodd med samboer minst ett år`() {
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            relasjon = OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer,
            harBoddSammenMinstEttÅr = false,
            barn = listOf(
                overføreOmsorgsdagerBarn(
                    aleneOmOmsorgen = true
                )
            )
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåPersonopplysningerOgSaksnummer(
            fra = fra,
            til = til
        )
        rapid.ventPå(antallMeldinger = 2)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }

}