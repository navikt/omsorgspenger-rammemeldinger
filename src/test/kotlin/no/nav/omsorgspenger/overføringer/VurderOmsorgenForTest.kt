package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.koronaoverføringer.rivers.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner
import no.nav.omsorgspenger.personopplysninger.TestRelasjon
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.ventPå
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class VurderOmsorgenForTest(
    dataSource: DataSource){

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(TestApplicationContextBuilder(
            dataSource = dataSource.cleanAndMigrate()
        ).build())
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Søker bor inte sammen med barn ger avslag`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = overføreOmsorgsdagerBarn()

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.of(2020, 12, 1),
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = barn.fødselsdato
            ))
        )

        rapid.sendTestMessage(behovssekvens)

        rapid.ventPå(1)

        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            relasjoner = setOf(
                TestRelasjon(identitetsnummer = barn.identitetsnummer, relasjon = "barn", borSammen = true),
                TestRelasjon(identitetsnummer = til, borSammen = false)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }

    @Test
    fun `Søker bor inte sammen med mottaker ger avslag`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = overføreOmsorgsdagerBarn()

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.of(2020, 12, 1),
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = barn.fødselsdato
            ))
        )

        rapid.sendTestMessage(behovssekvens)

        rapid.ventPå(1)

        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            relasjoner = setOf(
                TestRelasjon(identitetsnummer = barn.identitetsnummer, relasjon = "barn", borSammen = true),
                TestRelasjon(identitetsnummer = til, borSammen = false)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())
    }
}