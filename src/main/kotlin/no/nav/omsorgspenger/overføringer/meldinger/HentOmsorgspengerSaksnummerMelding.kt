package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer

internal object HentOmsorgspengerSaksnummerMelding :
    LeggTilBehov<HentOmsorgspengerSaksnummerMelding.BehovInput>,
    HentLøsning<Map<Identitetsnummer,Saksnummer>> {
    internal const val HentOmsorgspengerSaksnummer = "HentOmsorgspengerSaksnummer"
    private const val SaksnummerKey = "@løsninger.$HentOmsorgspengerSaksnummer.saksnummer"

    override fun behov(behovInput: BehovInput)  = Behov(
        navn = HentOmsorgspengerSaksnummer,
        input = mapOf(
            "identitetsnummer" to behovInput.identitetsnummer
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(SaksnummerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Map<Identitetsnummer, Saksnummer> {
        return (packet[SaksnummerKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { Pair(it.key, it.value.asText())}
            .toMap()
    }

    internal data class BehovInput(
        val identitetsnummer: Set<Identitetsnummer>
    )
}