package no.nav.omsorgspenger.koronaoverføringer.rivers

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.overføring
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.registerOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class BehandleKoronaOverføringerTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerOverføreKoronaOmsorgsdager(
            TestApplicationContextBuilder(
                dataSource = dataSource.cleanAndMigrate(),
                additionalEnv = mapOf("KORONA_BEHANDLING" to "enabled")
            ).also { builder ->
                builder.spleisetOverføringerService = overføringerMock
            }.build()
        )
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

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(koronaBarn())
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.mockHentOmsorgspengerSaksnummer(
            fra = fra,
            til = til
        )
        rapid.ventPå(2)
        rapid.mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(3)
        val (idSlutt, løsning) = rapid.løsningOverføreKoronaOmsorgsdager()
        assertEquals("Gjennomført", løsning.utfall)
        assertEquals(idStart, idSlutt)
    }

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