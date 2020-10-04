package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.overføringer.Part
import no.nav.omsorgspenger.overføringer.Part.Companion.somPart

internal object HentPersonopplysningerMelding :
    LeggTilBehov<HentPersonopplysningerMelding.BehovInput>,
    HentLøsning<Set<Part>> {
    internal const val HentPersonopplysninger = "HentPersonopplysninger"
    private const val IdentitetsnummerKey = "@løsninger.$HentPersonopplysninger.identitetsnummer"

    override fun behov(behovInput: BehovInput) = Behov(
        navn = HentPersonopplysninger,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "attributter" to setOf("navn", "fødseldato")
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(IdentitetsnummerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Set<Part> {
         return(packet[IdentitetsnummerKey] as ObjectNode)
             .fields()
             .asSequence()
             .map { it.toPair().somPart() }
             .toSet()
    }

    internal data class BehovInput(
        val identitetsnummer: Set<Identitetsnummer>
    )
}