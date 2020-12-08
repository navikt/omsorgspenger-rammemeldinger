package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
internal class IngenDagerTilgjengeligInneværendeÅrTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerApplicationContext(
            TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build()
        )
    }

    @BeforeEach
    fun reset(){
        rapid.reset()
    }

    @Test
    fun `Ingen dager tilgjengelige for overføring inneværende år`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barnetsFødselsdato = LocalDate.parse("2019-09-29")
        val mottaksdato = LocalDate.parse("2020-01-15")

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 20,
            omsorgsdagerÅOverføre = 9,
            mottaksdato = mottaksdato,
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = barnetsFødselsdato
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
                Periode("2021-01-01/2031-12-31") to 9 // 1.Januar året etter mottaksdato til ut året barnet fyller 12
            )
        )
    }
}