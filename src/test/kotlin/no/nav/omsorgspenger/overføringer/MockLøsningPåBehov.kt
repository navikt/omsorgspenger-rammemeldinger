package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger

internal fun TestRapid.mockLøsningPåPersonopplysningerOgSaksnummer(fra: Identitetsnummer, til: Identitetsnummer) {
    sendTestMessage(
        sisteMelding()
            .somJsonMessage()
            .mockLøsningPåHenteOmsorgspengerSaksnummer(fra, til)
            .mockLøsningPåHentePersonopplysninger(fra, til)
            .toJson()
    )
}

private fun JsonMessage.mockLøsningPåHenteOmsorgspengerSaksnummer(
    fra: Identitetsnummer, til: Identitetsnummer) = leggTilLøsning(
        behov = HentOmsorgspengerSaksnummerMelding.Navn,
        løsning = mapOf(
            "identitetsnummer" to mapOf(
                fra to "foo",
                til to "bar"
            )
        )
    )

private fun JsonMessage.mockLøsningPåHentePersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer) = leggTilLøsning(
        behov = HentPersonopplysninger,
        løsning = mapOf(
            "identitetsnummer" to mapOf(
                fra to mapOf(
                    "navn" to mapOf(
                        "fornavn" to "Ola",
                        "etternavn" to "Nordmann"
                    ),
                    "fødselsdato" to "1990-09-01"
                ),
                til to mapOf(
                    "navn" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "Persdatter",
                        "etternavn" to "Nordmann"
                    ),
                    "fødselsdato" to "1992-09-01"
                )
            )
        )
    )

internal fun TestRapid.sisteMelding() = inspektør.message(inspektør.size - 1).toString()

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }