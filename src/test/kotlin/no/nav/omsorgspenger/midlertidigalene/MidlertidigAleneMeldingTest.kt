package no.nav.omsorgspenger.midlertidigalene

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.MidlertidigAleneBehov
import no.nav.omsorgspenger.midlertidigalene.rivers.MidlertidigAleneMelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

internal class MidlertidigAleneMeldingTest {

    @Test
    fun `Håndterer gyldig behov`(){
        val mottatt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z")
        val jsonMessage = behovssekvens(mottatt = mottatt)
        MidlertidigAleneMelding.validateBehov(jsonMessage)
        val melding = MidlertidigAleneMelding.hentBehov(jsonMessage)
        assertEquals(mottatt, melding.mottatt)
        assertEquals("11111111111", melding.søker)
        assertEquals("22222222222", melding.annenForelder)
        assertThat(setOf("4111111")).hasSameElementsAs(melding.journalpostIder)
        assertEquals("1.0.0", melding.versjon)
    }
    
    private fun behovssekvens(mottatt: ZonedDateTime) = Behovssekvens(
        id = "01EQGMQ95AMMQD7PNQJDA3QSTF",
        correlationId = UUID.randomUUID().toString(),
        behov = arrayOf(MidlertidigAleneBehov(
            søker = MidlertidigAleneBehov.Person("11111111111"),
            annenForelder = MidlertidigAleneBehov.Person("22222222222"),
            journalpostIder = listOf("4111111"),
            mottatt = mottatt
        ))
    ).keyValue.second.let { JsonMessage(it, MessageProblems(it)) }
}