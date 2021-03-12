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
internal class KoronaOverføringPåVentTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerOverføreKoronaOmsorgsdager(
            TestApplicationContextBuilder(
                dataSource = dataSource.cleanAndMigrate()
            ).build()
        )
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Søknad for 2020 blir sendt som Gosysoppgave`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = koronaBarn()

        val (id, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = 10,
            periode = Periode("2020-06-01/2020-12-31"),
            journalpostIder = listOf("12345"),
            barn = listOf(barn)
        )
        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.sisteMeldingSomJSONObject().assertGosysJournalføringsoppgave(
            behovssekvensId = id,
            fra = fra,
            til = til,
            barn = barn.identitetsnummer,
            journalpostId = "12345"
        )
    }
}