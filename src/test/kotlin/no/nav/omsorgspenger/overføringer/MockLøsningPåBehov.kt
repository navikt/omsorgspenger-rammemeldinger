package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.overføringer.Behov.HentOmsorgspengerSaksnummer

internal fun TestRapid.sisteMelding() = inspektør.message(inspektør.size - 1).toString()

internal fun TestRapid.mockLøsningPåHenteOmsorgspengerSaksnummer(saksnummer: String) {
    sendTestMessage(
        sisteMelding()
            .somJsonMessage()
            .leggTilLøsning(
                behov = HentOmsorgspengerSaksnummer,
                løsning = mapOf("saksnummer" to saksnummer)
            ).toJson()
    )
}

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }