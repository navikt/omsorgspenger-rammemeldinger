package no.nav.omsorgspenger.fordelinger

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.FordeleOmsorgsdagerBehov
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.sisteMelding
import no.nav.omsorgspenger.testutils.ventPå
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert

@ExtendWith(DataSourceExtension::class)
internal class InitierFordelingAvOmsorgsdagerTest(
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
    fun `Søknader om fordeling av omsorgsdager skal alltid bli Gosys Journalføringsoppgaver`() {
        val behovssekvens = behovssekvensFordeling(ZonedDateTime.now())

        val håndtert = JSONObject(rapid.sendFordelingOmsorgsdagerOgHentHåndtertMelding(behovssekvens))

        @Language("JSON")
        val forventetOpprettGosysJournalføringsoppgaverBehov = """
        {
			"identitetsnummer": "11111111111",
			"berørteIdentitetsnummer": ["22222222222", "11111111122", "22222222211"],
			"journalpostIder": ["4111111"],
			"journalpostType": "FordeleOmsorgsdager"
		}
        """.trimIndent()

        assertEquals(listOf("FordeleOmsorgsdager", "OpprettGosysJournalføringsoppgaver"), håndtert.getJSONArray("@behovsrekkefølge").map { it.toString() })
        assertEquals(1, håndtert.getJSONObject("@løsninger").keySet().size)
        assertEquals("GosysJournalføringsoppgaver",håndtert.getJSONObject("@løsninger").getJSONObject("FordeleOmsorgsdager").getString("utfall"))
        JSONAssert.assertEquals(forventetOpprettGosysJournalføringsoppgaverBehov, håndtert.getJSONObject("@behov").getJSONObject("OpprettGosysJournalføringsoppgaver").toString(), true)
    }

    private fun TestRapid.sendFordelingOmsorgsdagerOgHentHåndtertMelding(
        behovssekvens: String): String {
        sendTestMessage(behovssekvens)
        ventPå(antallMeldinger = 1)
        return sisteMelding()
    }


    private fun behovssekvensFordeling(mottatt: ZonedDateTime) = Behovssekvens(
        id = "01EQGMQ95AMMQD7PNQJDA3QSTF",
        correlationId = UUID.randomUUID().toString(),
        behov = arrayOf(FordeleOmsorgsdagerBehov(
            fra = FordeleOmsorgsdagerBehov.Fra("11111111111"),
            til = FordeleOmsorgsdagerBehov.Til("22222222222"),
            barn = listOf(
                FordeleOmsorgsdagerBehov.Barn("11111111122", LocalDate.of(1999,1, 1)),
                FordeleOmsorgsdagerBehov.Barn("22222222211", LocalDate.of(1999,1, 1))
            ),
            journalpostIder = listOf("4111111"),
            mottatt = mottatt
        ))
    ).keyValue.second
}