package no.nav.omsorgspenger.overføringer

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class HarMidlertidigAleneVedtakTest(
    dataSource: DataSource){
    private val midlertidigAleneService = mockk<MidlertidigAleneService>()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(TestApplicationContextBuilder(
            dataSource = dataSource.cleanAndMigrate()
        ).also { builder ->
            builder.midlertidigAleneService = midlertidigAleneService
        }.build())
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

        every { midlertidigAleneService.hentMidlertidigAleneVedtak(any(), any(), any()) }
            .returns(listOf(MidlertidigAleneVedtak(
                periode = Periode("2017-02-15/2025-04-04"),
                kilder = setOf()
            )))
        val barn = overføreOmsorgsdagerBarn(
            aleneOmOmsorgen = true,
            fødselsdato = barnetsFødselsdato
        )
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = mottaksdato,
            barn = listOf(barn)
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til,
            barn = setOf(barn.identitetsnummer),
            borsammen = true
        )

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

        every { midlertidigAleneService.hentMidlertidigAleneVedtak(any(), any(), any()) }
            .returns(listOf(MidlertidigAleneVedtak(
                periode = Periode("2016-09-29/2028-12-31"),
                kilder = setOf()
            )))

        val barn = overføreOmsorgsdagerBarn(
            aleneOmOmsorgen = true,
            fødselsdato = barnetsFødselsdato
        )

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = mottaksdato,
            barn = listOf(barn)
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til,
            barn = setOf(barn.identitetsnummer),
            borsammen = true
        )

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }
}