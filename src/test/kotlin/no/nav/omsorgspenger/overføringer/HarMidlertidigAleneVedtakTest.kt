package no.nav.omsorgspenger.overføringer

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HarMidlertidigAleneVedtakTest {
    private val midlertidigAleneService = mockk<MidlertidigAleneService>()

    private val rapid = TestRapid().apply {
        AppBuilderMedDefaultMocks().also { appBuilder ->
            appBuilder.midlertidigAleneService = midlertidigAleneService
        }.build(this)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }


    @Test
    fun `Har midlertidig alene vedtak i deler av perioden`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val mottaksdato = LocalDate.parse("2016-09-29")
        val barnetsFødselsdato = LocalDate.parse("2016-04-29")

        every { midlertidigAleneService.hentMidlertidigAleneVedtak(any(), any()) }
            .returns(listOf(MidlertidigAleneVedtak(
                periode = Periode("2017-02-15/2025-04-04"),
                kilder = setOf()
            )))

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = mottaksdato,
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = barnetsFødselsdato
            ))
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåPersonopplysningerOgSaksnummer(
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
                Periode("2016-09-29/2017-02-14") to 10, // Fra mottaksdato til dagen før midl.alene vedtak gjelder fom
                Periode("2025-04-05/2028-12-31") to 10 // Fra dagen etter midl.alene vetak gjelder tom til ut året barnet fyller 12
            )
        )
    }

    @Test
    fun `Har midlertidig alene vedtak i hele perioden`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val mottaksdato = LocalDate.parse("2016-09-29")
        val barnetsFødselsdato = LocalDate.parse("2016-04-29")

        every { midlertidigAleneService.hentMidlertidigAleneVedtak(any(), any()) }
            .returns(listOf(MidlertidigAleneVedtak(
                periode = Periode("2016-09-29/2028-12-31"),
                kilder = setOf()
            )))

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = mottaksdato,
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = barnetsFødselsdato
            ))
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