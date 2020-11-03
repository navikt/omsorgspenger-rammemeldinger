package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class FødseldatoPåBarnTest(
    private val dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerApplicationContext(TestAppliationContextBuilder().also { builder ->
            builder.dataSource = dataSource
        }.build())
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Barn født etter mottaksdato`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val mottaksdato = LocalDate.parse("2020-09-29")
        val barnetsFødselsdato = LocalDate.parse("2021-02-15")

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = mottaksdato,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(overføreOmsorgsdagerBarn(
                fødselsdato = barnetsFødselsdato,
                aleneOmOmsorgen = true
            ))
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2021-02-15/2033-12-31") to 10 // Fra barnet er født til ut året det fyller 12
            )
        )
    }

    @Test
    fun `Barn utenfor periode for omsorgen for`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val mottaksdato = LocalDate.parse("2020-09-29")

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = mottaksdato,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(
                overføreOmsorgsdagerBarn(
                    fødselsdato = mottaksdato.minusYears(13),
                    aleneOmOmsorgen = true
                )
            )
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }
}