package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.koronaoverføringer.rivers.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner
import no.nav.omsorgspenger.overføringer.formidling.GittDager
import no.nav.omsorgspenger.overføringer.formidling.Grunn
import no.nav.omsorgspenger.overføringer.statistikk.StatistikkFormat.assertForventetAvslagSkjermet
import no.nav.omsorgspenger.overføringer.statistikk.StatistikkFormat.assertForventetAvslag
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.RecordingFormidlingService
import no.nav.omsorgspenger.testutils.RecordingStatistikkService
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.ventPå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class VurderOmsorgenForTest(
    dataSource: DataSource) {

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate()
    ).build()

    private val formidlingService = applicationContext.formidlingService as RecordingFormidlingService
    private val statistikkService = applicationContext.statistikkService as RecordingStatistikkService

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
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

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2020-12-01"),
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
                VurderRelasjonerMelding.Relasjon(identitetsnummer = barn.identitetsnummer, relasjon = "BARN", borSammen = false),
                VurderRelasjonerMelding.Relasjon(identitetsnummer = til, relasjon = "INGEN", borSammen = true)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())

        formidlingService.finnMeldingsbestillingerFor(behovssekvensId).assertAvslagsmelding(Grunn.IKKE_OMSORGEN_FOR_NOEN_BARN)
        statistikkService.finnStatistikkMeldingFor(behovssekvensId).assertForventetAvslag(behovssekvensId)
    }

    @Test
    @Disabled("TODO: Må rettes opp så fremt vi kan ende IKKE_OMSORGEN_FOR_NOEN_BARN-brevet i dette scenarioet")
    fun `Bor ikke sammen med barn med aleneomsorg, men har omsorgen for et annet barn på samme adresse`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = overføreOmsorgsdagerBarn(
            aleneOmOmsorgen = true
        )
        val barn2 = overføreOmsorgsdagerBarn(
            aleneOmOmsorgen = false
        )

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2020-12-01"),
            barn = listOf(barn, barn2)
        )

        rapid.sendTestMessage(behovssekvens)

        rapid.ventPå(1)

        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            relasjoner = setOf(
                VurderRelasjonerMelding.Relasjon(identitetsnummer = barn.identitetsnummer, relasjon = "BARN", borSammen = false),
                VurderRelasjonerMelding.Relasjon(identitetsnummer = barn2.identitetsnummer, relasjon = "BARN", borSammen = true),
                VurderRelasjonerMelding.Relasjon(identitetsnummer = til, relasjon = "INGEN", borSammen = true)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())

        formidlingService.finnMeldingsbestillingerFor(behovssekvensId).assertAvslagsmelding(Grunn.IKKE_OMSORGEN_FOR_NOEN_BARN)
        statistikkService.finnStatistikkMeldingFor(behovssekvensId).assertForventetAvslag(behovssekvensId)
    }

    @Test
    fun `Søker bor inte sammen med mottaker ger avslag`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = overføreOmsorgsdagerBarn()

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2020-12-01"),
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
                VurderRelasjonerMelding.Relasjon(identitetsnummer = barn.identitetsnummer, relasjon = "BARN", borSammen = true),
                VurderRelasjonerMelding.Relasjon(identitetsnummer = til, relasjon = "INGEN", borSammen = false)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())

        formidlingService.finnMeldingsbestillingerFor(behovssekvensId).assertAvslagsmelding(Grunn.IKKE_SAMME_ADRESSE_SOM_MOTTAKER)
        statistikkService.finnStatistikkMeldingFor(behovssekvensId).assertForventetAvslag(behovssekvensId)

    }

    @Test
    fun `Søker bor hverken med barn eller mottaker gir avslag`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = overføreOmsorgsdagerBarn()

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2020-12-01"),
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
                VurderRelasjonerMelding.Relasjon(identitetsnummer = barn.identitetsnummer, relasjon = "INGEN", borSammen = false),
                VurderRelasjonerMelding.Relasjon(identitetsnummer = til, relasjon = "INGEN", borSammen = false)
            )
        )

        rapid.ventPå(2)

        rapid.mockLøsningPåHentePersonopplysninger(fra=fra, til=til, skjermetTil = true)

        rapid.ventPå(3)

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erAvslått())

        assertEquals(0, formidlingService.finnMeldingsbestillingerFor(behovssekvensId).size) // Ettersom en part er skjemet blir det ikke brev
        statistikkService.finnStatistikkMeldingFor(behovssekvensId).assertForventetAvslagSkjermet(behovssekvensId)
    }

    private fun List<Meldingsbestilling>.assertAvslagsmelding(avslagsGrunn: Grunn) {
        assertThat(this).hasSize(1)
        val forventetStartOgSluttGrunn = avslagsGrunn to avslagsGrunn
        val gitt = this.first { it.melding is GittDager }.melding as GittDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertTrue(gitt.formidlingsoverføringer.avslått)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).isEmpty()
        this.forEach { println(it.keyValue.second) }
    }
}