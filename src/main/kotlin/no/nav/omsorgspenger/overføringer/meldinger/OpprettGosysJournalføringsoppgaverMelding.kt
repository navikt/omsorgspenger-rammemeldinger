package no.nav.omsorgspenger.overføringer.meldinger

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
            "identitetsnummer" to behovInput.fra,
            "berørteIdentitetsnummer" to setOf(behovInput.til),
            "journalpostIder" to behovInput.journalpostIder,
            "journalpostType" to "OverføreOmsorgsdager"
        )
    )

    internal data class BehovInput(
        val fra: Identitetsnummer,
        val til: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )
}