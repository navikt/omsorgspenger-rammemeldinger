package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.Fellesopplysnigner
import no.nav.omsorgspenger.overføringer.Personopplysninger
import no.nav.omsorgspenger.overføringer.Personopplysninger.Companion.somPersonopplysninger
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object HentPersonopplysningerMelding :
    LeggTilBehov<HentPersonopplysningerMelding.BehovInput>,
    HentLøsning<Pair<Map<Identitetsnummer, Personopplysninger>, Fellesopplysnigner>> {
    internal const val HentPersonopplysninger = "HentPersonopplysninger"
    private const val PersonopplysningerKey = "@løsninger.$HentPersonopplysninger.personopplysninger"
    private const val FellesopplysningerKey = "@løsninger.$HentPersonopplysninger.fellesopplysninger"


    override fun behov(behovInput: BehovInput) = Behov(
        navn = HentPersonopplysninger,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "attributter" to setOf("navn", "fødselsdato", "aktørId", "adressebeskyttelse", "gjeldendeIdentitetsnummer", "enhetsnummer")
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(PersonopplysningerKey)
        packet.interestedIn(FellesopplysningerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Pair<Map<Identitetsnummer, Personopplysninger>, Fellesopplysnigner> {
         val personopplysninger = (packet[PersonopplysningerKey] as ObjectNode)
             .fields()
             .asSequence()
             .map { it.key to it.toPair().somPersonopplysninger() }
             .toMap()
        val fellesopplysnigner = Fellesopplysnigner(
            (packet[FellesopplysningerKey] as ObjectNode).get("enhetsnummer").asText()
        )

        return personopplysninger to fellesopplysnigner
    }

    internal data class BehovInput(
        val identitetsnummer: Set<Identitetsnummer>
    )
}