package no.nav.omsorgspenger.overføringer

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.omsorgspenger.Periode
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
internal class FødselsdatoPåBarnTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerApplicationContext(
            TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build()
        )
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

        val barn = overføreOmsorgsdagerBarn(
            fødselsdato = barnetsFødselsdato,
            aleneOmOmsorgen = true
        )
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = mottaksdato,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
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
                Periode("2021-02-15/2033-12-31") to 10 // Fra barnet er født til ut året det fyller 12
            )
        )
    }

    @Test
    fun `Barn utenfor periode for omsorgen for`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val mottaksdato = LocalDate.parse("2020-09-29")

        val barn = overføreOmsorgsdagerBarn(
            fødselsdato = mottaksdato.minusYears(13),
            aleneOmOmsorgen = true
        )
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = mottaksdato,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
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