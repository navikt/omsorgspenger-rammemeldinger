package no.nav.omsorgspenger.rivers.meldinger

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
            "identitetsnummer" to behovInput.identitetsnummer,
            "berørteIdentitetsnummer" to behovInput.berørteIdentitetsnummer,
            "journalpostIder" to behovInput.journalpostIder,
            "journalpostType" to behovInput.journalpostType
        )
    )

    internal data class BehovInput(
        val identitetsnummer: Identitetsnummer,
        val berørteIdentitetsnummer: Set<Identitetsnummer>,
        val journalpostIder: Set<JournalpostId>,
        val journalpostType: String
    )
}