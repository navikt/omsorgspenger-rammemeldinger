package no.nav.omsorgspenger.personopplysninger

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object VurderRelasjonerMelding :
    LeggTilBehov<VurderRelasjonerMelding.BehovInput>,
    HentLøsning<Set<VurderRelasjonerMelding.Relasjon>> {
    internal const val VurderRelasjoner = "VurderRelasjoner"
    private const val VurderRelasjonerKey = "@løsninger.$VurderRelasjoner.relasjoner"

    override fun behov(behovInput: BehovInput)  = Behov(
        navn = VurderRelasjoner,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer,
            "til" to behovInput.til
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(VurderRelasjonerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Set<Relasjon> {
        return (packet["@løsninger.VurderRelasjoner.relasjoner"] as ArrayNode)
            .map { it as ObjectNode }
            .map { Relasjon(
                identitetsnummer = it["identitetsnummer"].asText(),
                relasjon = it["relasjon"].asText(),
                borSammen = it["borSammen"].asBoolean()) }
            .toSet()
    }

    internal data class BehovInput(
        val identitetsnummer: String,
        val til: Set<Identitetsnummer>
    )

    internal data class Relasjon(
        val relasjon: String,
        val identitetsnummer: String,
        val borSammen: Boolean
    )
}