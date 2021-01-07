package no.nav.omsorgspenger.koronaoverføringer.rivers

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.overføring
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.registerOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(DataSourceExtension::class)
internal class BehandleKoronaOverføringerTest(
    dataSource: DataSource) {
    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        additionalEnv = mapOf("KORONA_BEHANDLING" to "enabled")
    ).also { builder ->
        builder.spleisetOverføringerService = overføringerMock
    }.build()

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
        // TODO
    }

    @Test
    fun `Gjennomført overføring`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val barnet = koronaBarn()

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(barnet)
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

        rapid.mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(3)
        val (idSlutt, løsning) = rapid.løsningOverføreKoronaOmsorgsdager()
        assertTrue(løsning.erGjennomført())
        assertEquals(idStart, idSlutt)

        hentKoronaoverføringerFor(fra).also {
            assertThat(it.fått).isEmpty()
            assertThat(it.gitt).hasSize(1)
        }
        hentKoronaoverføringerFor(til).also {
            assertThat(it.fått).hasSize(1)
            assertThat(it.gitt).isEmpty()
        }
    }

    @Test
    fun `Avslått overføring ifall alle barn ikke bor på samme adresse`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = koronaBarn()

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(barn)
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            barn = setOf(barn.identitetsnummer),
            borSammen = false
        )
        rapid.ventPå(2)

        rapid.mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(3)
        val (idSlutt, løsning) = rapid.løsningOverføreKoronaOmsorgsdager()
        assertTrue(løsning.erAvslått())
        assertEquals(idStart, idSlutt)
    }

    private fun hentKoronaoverføringerFor(identitetsnummer: Identitetsnummer) =
        applicationContext.spleisetKoronaOverføringService.hentSpleisetOverføringer(
            identitetsnummer = identitetsnummer,
            periode = Periode("2021-01-01/2021-12-31"),
            correlationId = "test"
        )

    private companion object {
        private val overføringerMock = mockk<SpleisetOverføringerService>().also {
            every { it.hentSpleisetOverføringer(any(), any(), any()) }
                .returns(SpleisetOverføringer(
                    gitt = listOf(overføring(
                        periode = Periode("2021-01-01/2021-12-31"),
                        antallDager = 7
                    )),
                    fått = emptyList()
                ))
        }
    }
}