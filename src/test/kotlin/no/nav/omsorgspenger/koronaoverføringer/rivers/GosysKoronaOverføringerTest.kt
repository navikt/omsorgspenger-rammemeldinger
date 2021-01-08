package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.registerOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.sisteMeldingSomJSONObject
import no.nav.omsorgspenger.testutils.ventPå
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class GosysKoronaOverføringerTest(
    dataSource: DataSource) {
    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        additionalEnv = mapOf("KORONA_BEHANDLING" to "enabled")
    ).build()

    private val rapid = TestRapid().apply {
        this.registerOverføreKoronaOmsorgsdager(applicationContext)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }


    @Test
    fun `Annen periode enn støttet blir til Gosys journalføringsoppgaver`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val (id, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = 10,
            periode = Periode("2020-06-01/2020-12-31"),
            journalpostIder = listOf("88124")
        )
        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.sisteMeldingSomJSONObject().assertGosysJournalføringsoppgave(
            behovssekvensId = id,
            fra = fra,
            til = til,
            journalpostId = "88124"
        )
    }

    @Test
    fun `Svart at man ikke jobber i Norge blir til Gosys journalføringsoppgaver`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val (id, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = 10,
            jobberINorge = false,
            journalpostIder = listOf("88126")
        )
        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.sisteMeldingSomJSONObject().assertGosysJournalføringsoppgave(
            behovssekvensId = id,
            fra = fra,
            til = til,
            journalpostId = "88126"
        )
    }

    @Test
    fun `Ikke funnet vedtak om utvidet rett som gjør at hele overføringen ikke kan gjennomføres blir til Gosys journalføringsoppgaver`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val barnet = koronaBarn(utvidetRett = true)

        val (id, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 21, // En dag mer enn man kan overføre uten utvidet rett
            barn = listOf(barnet),
            journalpostIder = listOf("88127")
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            barn = setOf(barnet.identitetsnummer),
            borSammen = true
        )
        rapid.ventPå(2)

        rapid.sisteMeldingSomJSONObject().assertGosysJournalføringsoppgave(
            behovssekvensId = id,
            fra = fra,
            til = til,
            journalpostId = "88127",
            forventetLøsninger = listOf(
                "HentFordelingGirMeldinger",
                "HentOverføringGirMeldinger",
                "HentKoronaOverføringGirMeldinger",
                "HentUtvidetRettVedtak",
                "HentOmsorgspengerSaksnummer",
                "VurderRelasjoner",
                "OverføreKoronaOmsorgsdagerBehandling",
                "OverføreKoronaOmsorgsdager"
            )
        )
    }
}