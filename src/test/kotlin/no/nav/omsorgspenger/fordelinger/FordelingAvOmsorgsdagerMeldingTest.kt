package no.nav.omsorgspenger.fordelinger

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.FordeleOmsorgsdagerBehov
import no.nav.omsorgspenger.fordelinger.meldinger.FordelingAvOmsorgsdagerMelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FordelingAvOmsorgsdagerMeldingTest {

    @Test
    fun `Håndterer gyldig behov`(){
        val mottatt = ZonedDateTime.parse("2020-11-10T12:00:00.00Z")
        val jsonMessage = behovssekvens(mottatt = mottatt)
        FordelingAvOmsorgsdagerMelding.validateBehov(jsonMessage)
        val melding = FordelingAvOmsorgsdagerMelding.hentBehov(jsonMessage)
        assertEquals(mottatt, melding.mottatt)
        assertEquals("11111111111", melding.fra)
        assertEquals("22222222222", melding.til)
        assertThat(setOf("4111111")).hasSameElementsAs(melding.journalpostIder)
        assertEquals("1.0.0", melding.versjon)
    }
    
    private fun behovssekvens(mottatt: ZonedDateTime) = Behovssekvens(
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
    ).keyValue.second.let { JsonMessage(it, MessageProblems(it)) }
}