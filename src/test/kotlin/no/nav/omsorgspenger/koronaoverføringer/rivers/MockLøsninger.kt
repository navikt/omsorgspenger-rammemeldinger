package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding.VurderRelasjoner
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.testutils.sisteMeldingSomJsonMessage

internal fun TestRapid.mockHentPersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer
) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
            .leggTilLøsningPåHentePersonopplysninger(fra = fra, til = til)
            .toJson()
    )
}

internal fun TestRapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
    fra: Identitetsnummer, til: Identitetsnummer,
    relasjoner: Set<VurderRelasjonerMelding.Relasjon> = emptySet()
) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
            .leggTilLøsningPåHenteOmsorgspengerSaksnummer(fra = fra, til = til)
            .leggTilLøsningPåVurderRelasjonerTilBarn(relasjoner = relasjoner)
            .toJson()
    )
}

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer
) = leggTilLøsning(
    behov = HentPersonopplysningerMelding.HentPersonopplysninger,
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
                "gjeldendeIdentitetsnummer" to fra,
                "enhetsnummer" to "4487",
                "enhetstype" to "VANLIG"
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
                "gjeldendeIdentitetsnummer" to til,
                "enhetsnummer" to "4487",
                "enhetstype" to "VANLIG"
            )
        )
    )
)

private fun JsonMessage.leggTilLøsningPåHenteOmsorgspengerSaksnummer(
    fra: Identitetsnummer, til: Identitetsnummer
) = leggTilLøsning(
    behov = HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
    løsning = mapOf(
        "saksnummer" to mapOf(
            fra to "foo",
            til to "bar"
        )
    )
)

private fun JsonMessage.leggTilLøsningPåVurderRelasjonerTilBarn(
    relasjoner: Set<VurderRelasjonerMelding.Relasjon>
) = leggTilLøsning(
    behov = VurderRelasjoner,
    løsning = mapOf(
        "relasjoner" to relasjoner.map {
            mapOf(
                "relasjon" to it.relasjon,
                "identitetsnummer" to it.identitetsnummer,
                "borSammen" to it.borSammen
            )
        }
    )
)
