package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Saksnummer

internal object FerdigstillJournalføringForOmsorgspengerMelding :
    LeggTilBehov<FerdigstillJournalføringForOmsorgspengerMelding.BehovInput> {

    internal const val FerdigstillJournalføringForOmsorgspenger = "FerdigstillJournalføringForOmsorgspenger"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = FerdigstillJournalføringForOmsorgspenger,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "journalpostIder" to behovInput.journalpostIder,
            "saksnummer" to behovInput.saksnummer
        )
    )

    internal data class BehovInput(
        val identitetsnummer: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>,
        val saksnummer: Saksnummer
    )
}