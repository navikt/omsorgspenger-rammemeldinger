package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class InngangsvilkårTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerApplicationContext(
            TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build()
        )
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
        rapid.mockLøsningPåHenteOmsorgspengerSaksnummer(fra = fra, til = til)
        rapid.ventPå(antallMeldinger = 2)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.ikkeBehandlesAvNyttSystem())
        assertTrue(løsning.overføringer.isEmpty())
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

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }

}