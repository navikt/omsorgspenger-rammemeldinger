package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.Personopplysninger
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.adressebeskyttet
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.aktørId
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.enhet
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.fødselsdato
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.gjeldendeIdentitetsnummer
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.navn

internal object OverføreOmsorgsdagerPersonopplysningerMelding
    : HentPersonopplysningerMelding<Personopplysninger, Any>(
    defaultAttributter = setOf(
        "navn",
        "fødselsdato",
        "adressebeskyttelse",
        "aktørId",
        "gjeldendeIdentitetsnummer",
        "enhetstype",
        "enhetsnummer")) {
    internal val HentPersonopplysninger = HentPersonopplysningerMelding.HentPersonopplysninger

    override fun mapPersonopplysninger(input: Map<Identitetsnummer, ObjectNode>): Map<Identitetsnummer, Personopplysninger> {
        return input.mapValues { (_,json) -> Personopplysninger(
            navn = json.navn(),
            fødselsdato = json.fødselsdato(),
            gjeldendeIdentitetsnummer = json.gjeldendeIdentitetsnummer(),
            adressebeskyttet = json.adressebeskyttet(),
            aktørId = json.aktørId(),
            enhet = json.enhet()
        )}
    }

    override fun mapFellesopplysninger(input: JsonNode) = Any()

    internal fun Map<Identitetsnummer, Personopplysninger>.fellesEnhet(
        fra: Identitetsnummer) : Enhet {
        val skjermetPersonopplysninger = filterValues { it.enhet.skjermet }.entries.firstOrNull()?.value
        val fraPersonopplysninger = getValue(fra)

        return when (skjermetPersonopplysninger) {
            null -> fraPersonopplysninger.enhet
            else -> skjermetPersonopplysninger.enhet
        }
    }
}