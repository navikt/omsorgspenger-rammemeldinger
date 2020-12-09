package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.personopplysninger.Navn
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.adressebeskyttet
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.aktørId
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.fødselsdato
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.navn
import java.time.LocalDate

internal object OverføreKoronaOmsorgsdagerPersonopplysningerMelding
    : HentPersonopplysningerMelding<OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger, Any>(
    defaultAttributter = setOf("navn", "fødseldato", "adressebeskyttelse", "aktørId")) {

    internal data class Personopplysninger(
        internal val navn: Navn?,
        internal val aktørId: AktørId,
        internal val fødselsdato: LocalDate,
        internal val adressebeskyttet: Boolean
    )

    override fun mapPersonopplysninger(input: Map<Identitetsnummer, ObjectNode>): Map<Identitetsnummer, Personopplysninger> {
        return input.mapValues { (_,json) ->  Personopplysninger(
            navn = json.navn(),
            aktørId = json.aktørId(),
            fødselsdato = json.fødselsdato(),
            adressebeskyttet = json.adressebeskyttet()
        )}
    }

    override fun mapFellesopplysninger(input: JsonNode) = Any()
}