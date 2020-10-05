package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId

internal object OpprettGosysJournalføringsoppgaverMelding :
    LeggTilBehov<OpprettGosysJournalføringsoppgaverMelding.BehovInput> {
    internal const val OpprettGosysJournalføringsoppgaver = "OpprettGosysJournalføringsoppgaver"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = OpprettGosysJournalføringsoppgaver,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "journalpostIder" to behovInput.journalpostIder,
            "journalpostType" to "OverføreOmsorgsdager"
        )
    )

    internal data class BehovInput(
        val identitetsnummer: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )
}