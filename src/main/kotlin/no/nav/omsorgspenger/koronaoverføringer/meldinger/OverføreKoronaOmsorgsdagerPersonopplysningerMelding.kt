package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.personopplysninger.Navn
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.adressebeskyttet
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.aktørId
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.enhet
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.fødselsdato
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.gjeldendeIdentitetsnummer
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.navn
import java.time.LocalDate

internal object OverføreKoronaOmsorgsdagerPersonopplysningerMelding
    : HentPersonopplysningerMelding<OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger, Any>(
    defaultAttributter = setOf(
        "navn",
        "fødselsdato",
        "adressebeskyttelse",
        "aktørId",
        "gjeldendeIdentitetsnummer",
        "enhetstype",
        "enhetsnummer"
    )) {
    internal val HentPersonopplysninger = HentPersonopplysningerMelding.HentPersonopplysninger

    internal data class Personopplysninger(
        internal val navn: Navn?,
        internal val aktørId: AktørId,
        internal val fødselsdato: LocalDate,
        internal val adressebeskyttet: Boolean,
        internal val gjeldendeIdentitetsnummer: Identitetsnummer,
        internal val enhet: Enhet
    )

    override fun mapPersonopplysninger(input: Map<Identitetsnummer, ObjectNode>): Map<Identitetsnummer, Personopplysninger> {
        return input.mapValues { (_,json) ->  Personopplysninger(
            navn = json.navn(),
            aktørId = json.aktørId(),
            fødselsdato = json.fødselsdato(),
            adressebeskyttet = json.adressebeskyttet(),
            gjeldendeIdentitetsnummer = json.gjeldendeIdentitetsnummer(),
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