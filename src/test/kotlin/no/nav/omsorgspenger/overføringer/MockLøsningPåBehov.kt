package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning

internal fun TestRapid.sisteMelding() = inspektør.message(inspektør.size - 1).toString()

internal fun TestRapid.mockLøsningPåHenteOmsorgspengerSaksnummer(saksnummer: String) {
    sendTestMessage(
        sisteMelding()
            .somJsonMessage()
            .leggTilLøsning(
                behov = HentOmsorgspengerSaksnummerMelding.Navn,
                løsning = mapOf("saksnummer" to saksnummer)
            ).toJson()
    )
}

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }