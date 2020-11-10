package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.Personopplysninger
import no.nav.omsorgspenger.overføringer.Personopplysninger.Companion.somPersonopplysninger

internal object HentPersonopplysningerMelding :
    LeggTilBehov<HentPersonopplysningerMelding.BehovInput>,
    HentLøsning<Map<Identitetsnummer, Personopplysninger>> {
    internal const val HentPersonopplysninger = "HentPersonopplysninger"
    private const val PersonopplysningerKey = "@løsninger.$HentPersonopplysninger.personopplysninger"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = HentPersonopplysninger,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "attributter" to setOf("navn", "fødselsdato", "aktørId", "adressebeskyttelse", "gjeldendeIdentitetsnummer")
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(PersonopplysningerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Map<Identitetsnummer, Personopplysninger> {
         return(packet[PersonopplysningerKey] as ObjectNode)
             .fields()
             .asSequence()
             .map { it.key to it.toPair().somPersonopplysninger() }
             .toMap()
    }

    internal data class BehovInput(
        val identitetsnummer: Set<Identitetsnummer>
    )
}