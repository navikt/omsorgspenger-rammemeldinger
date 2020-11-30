package no.nav.omsorgspenger.midlertidigalene.rivers

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object OpprettGosysJournalføringsoppgaverMelding :
    LeggTilBehov<OpprettGosysJournalføringsoppgaverMelding.BehovInput> {
    internal const val OpprettGosysJournalføringsoppgaver = "OpprettGosysJournalføringsoppgaver"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = OpprettGosysJournalføringsoppgaver,
        input = mapOf(
            "identitetsnummer" to behovInput.søker,
            "berørteIdentitetsnummer" to setOf(behovInput.annenForelder),
            "journalpostIder" to behovInput.journalpostIder,
            "journalpostType" to "MidlertidigAlene"
        )
    )

    internal data class BehovInput(
        val søker: Identitetsnummer,
        val annenForelder: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )
}