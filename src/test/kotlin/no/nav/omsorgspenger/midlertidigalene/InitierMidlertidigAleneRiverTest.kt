package no.nav.omsorgspenger.midlertidigalene

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.MidlertidigAleneBehov
import no.nav.omsorgspenger.overføringer.sisteMelding
import no.nav.omsorgspenger.overføringer.ventPå
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class InitierMidlertidigAleneRiverTest(
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
    fun `Søknader om midlertidig alene skal alltid bli Gosys Journalføringsoppgaver`() {
        val behovssekvens = behovssekvensMidlertidigAlene()

        val håndtert = JSONObject(rapid.sendMidlertidigAleneOgHentHåndtertMelding(behovssekvens))

        @Language("JSON")
        val forventetOpprettGosysJournalføringsoppgaverBehov = """
        {
			"identitetsnummer": "11111111112",
			"berørteIdentitetsnummer": ["22222222223"],
			"journalpostIder": ["4111112"],
			"journalpostType": "MidlertidigAlene"
		}
        """.trimIndent()

        assertEquals(listOf("MidlertidigAlene", "OpprettGosysJournalføringsoppgaver"), håndtert.getJSONArray("@behovsrekkefølge").map { it.toString() })
        assertEquals(1, håndtert.getJSONObject("@løsninger").keySet().size)
        assertEquals("GosysJournalføringsoppgaver",håndtert.getJSONObject("@løsninger").getJSONObject("MidlertidigAlene").getString("utfall"))
        JSONAssert.assertEquals(forventetOpprettGosysJournalføringsoppgaverBehov, håndtert.getJSONObject("@behov").getJSONObject("OpprettGosysJournalføringsoppgaver").toString(), true)
    }

    private fun TestRapid.sendMidlertidigAleneOgHentHåndtertMelding(
        behovssekvens: String): String {
        sendTestMessage(behovssekvens)
        ventPå(antallMeldinger = 1)
        return sisteMelding()
    }

    private fun behovssekvensMidlertidigAlene() = no.nav.k9.rapid.behov.Behovssekvens(
        id = "01ERCHYSS811952JR0FK5C1DNN",
        correlationId = UUID.randomUUID().toString(),
        behov = arrayOf(MidlertidigAleneBehov(
            søker = MidlertidigAleneBehov.Person("11111111112"),
            annenForelder = MidlertidigAleneBehov.Person("22222222223"),
            journalpostIder = listOf("4111112"),
            mottatt = ZonedDateTime.now()
        ))
    ).keyValue.second
}