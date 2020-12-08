package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.OverføreKoronaOmsorgsdagerBehov
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.IdentitetsnummerGenerator
import no.nav.omsorgspenger.overføringer.ventPå
import no.nav.omsorgspenger.registerOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.sisteMelding
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class BehandleKoronaOverføringerTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerOverføreKoronaOmsorgsdager(
            TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build()
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
        JSONObject(rapid.sisteMelding()).assertGosysJournalføringsoppgave(
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
        JSONObject(rapid.sisteMelding()).assertGosysJournalføringsoppgave(
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
    fun `Innvilget overføring`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val (id, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(OverføreKoronaOmsorgsdagerBehov.Barn(
                identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
                fødselsdato = LocalDate.now().minusYears(1),
                aleneOmOmsorgen = false,
                utvidetRett = false
            ))
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.sendTestMessage(rapid.sisteMelding().somJsonMessage().leggTilLøsningPåHenteOmsorgspengerSaksnummer(
            fra = fra,
            til = til
        ).toJson())
    }
}