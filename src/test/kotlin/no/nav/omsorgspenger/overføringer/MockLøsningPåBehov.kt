package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer

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

internal fun TestRapid.mockLøsningPåHentePersonopplysninger(fra: Identitetsnummer, til: Identitetsnummer) {
    sendTestMessage(
        sisteMelding()
            .somJsonMessage()
            .leggTilLøsning(
                behov = HentPersonopplysningerMelding.Navn,
                løsning = mapOf(
                    "identitetsnummer" to mapOf(
                        fra to mapOf(
                            "navn" to "Ola",
                            "fødselsdato" to "1990-09-01"
                        ),
                        til to mapOf(
                            "navn" to "Kari",
                            "fødselsdato" to "1992-09-01"
                        )
                    )
                )
            ).toJson()
    )
}

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }