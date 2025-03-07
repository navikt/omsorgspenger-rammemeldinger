package no.nav.omsorgspenger.overføringer

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.testutils.sisteMeldingSomJsonMessage

internal fun TestRapid.mockLøsningPåHenteOmsorgspengerSaksnummerOchVurderRelasjoner(
    fra: Identitetsnummer, til: Identitetsnummer, barn: Set<Identitetsnummer> = emptySet() ,borSammen: Boolean = true) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
        .leggTilLøsningPåHenteOmsorgspengerSaksnummer(fra, til)
        .leggTilLøsningPåVurderRelasjoner(barn.plus(til), borSammen)
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
    fra: Identitetsnummer, til: Identitetsnummer, skjermetTil: Boolean = false) {
    sendTestMessage(
        sisteMeldingSomJsonMessage()
        .leggTilLøsningPåHentePersonopplysninger(fra, til, skjermetTil)
        .toJson()
    )
}

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger(
    fra: Identitetsnummer, til: Identitetsnummer, skjermetTil: Boolean) = leggTilLøsning(
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
                    "adressebeskyttelse" to when (skjermetTil) {
                        true -> "STRENGT_FORTROLIG"
                        false -> "UGRADERT"
                   },
                    "gjeldendeIdentitetsnummer" to til,
                    "enhetsnummer" to when (skjermetTil) {
                        true -> "2103"
                        false -> "4487"
                    },
                    "enhetstype" to when (skjermetTil) {
                        true -> "SKJERMET"
                        false -> "VANLIG"
                    }
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