package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.testutils.sisteMeldingSomJsonMessage

internal fun TestRapid.mockLøsningPåHenteOmsorgspengerSaksnummer(
    fra: Identitetsnummer, til: Identitetsnummer, borSammen: Boolean = true) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
        .leggTilLøsningPåHenteOmsorgspengerSaksnummer(fra, til)
        .leggTilLøsningPåVurderRelasjoner(setOf(til), borSammen)
        .toJson()
    )
}

private fun JsonMessage.leggTilLøsningPåHenteOmsorgspengerSaksnummer(
    fra: Identitetsnummer, til: Identitetsnummer) = leggTilLøsning(
        behov = HentOmsorgspengerSaksnummer,
        løsning = mapOf(
            "saksnummer" to mapOf(
                fra to "foo",
                til to "bar"
            )
        )
    )

internal fun TestRapid.mockLøsningPåHentePersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
        .leggTilLøsningPåHentePersonopplysninger(fra, til)
        .toJson()
    )
}

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer) = leggTilLøsning(
        behov = HentPersonopplysninger,
        løsning = mapOf(
            "personopplysninger" to mapOf(
                fra to mapOf(
                    "navn" to mapOf(
                        "fornavn" to "Ola",
                        "etternavn" to "Nordmann"
                    ),
                    "fødselsdato" to "1990-09-01",
                    "aktørId" to "33",
                    "adressebeskyttelse" to "UGRADERT",
                    "gjeldendeIdentitetsnummer" to fra
                ),
                til to mapOf(
                    "navn" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "Persdatter",
                        "etternavn" to "Nordmann"
                    ),
                    "fødselsdato" to "1992-09-01",
                    "aktørId" to "44",
                    "adressebeskyttelse" to "UGRADERT",
                    "gjeldendeIdentitetsnummer" to til
                )
            )
        )
    )

private fun JsonMessage.leggTilLøsningPåVurderRelasjoner(
    til: Set<Identitetsnummer>,
    borSammen: Boolean
) = leggTilLøsning(
    behov = VurderRelasjonerMelding.VurderRelasjoner,
    løsning = mapOf(
        "relasjoner" to til.map {
            mapOf(
                "relasjon" to "BARN",
                "identitetsnummer" to it,
                "borSammen" to borSammen
            )
        }
    )
)