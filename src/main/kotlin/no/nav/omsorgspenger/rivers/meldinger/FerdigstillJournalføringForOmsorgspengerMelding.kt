package no.nav.omsorgspenger.rivers.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object FerdigstillJournalføringForOmsorgspengerMelding :
    LeggTilBehov<FerdigstillJournalføringForOmsorgspengerMelding.BehovInput> {

    internal const val FerdigstillJournalføringForOmsorgspenger = "FerdigstillJournalføring"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = FerdigstillJournalføringForOmsorgspenger,
        input = mapOf(
            "versjon" to "1.0.0",
            "identitetsnummer" to behovInput.identitetsnummer,
            "journalpostIder" to behovInput.journalpostIder,
            "saksnummer" to behovInput.saksnummer,
            "fagsystem" to "OMSORGSPENGER"
        )
    )

    internal data class BehovInput(
        val identitetsnummer: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>,
        val saksnummer: Saksnummer
    )
}