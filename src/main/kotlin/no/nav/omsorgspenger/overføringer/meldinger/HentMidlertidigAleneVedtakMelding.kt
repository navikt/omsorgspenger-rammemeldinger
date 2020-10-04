package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak

internal object HentMidlertidigAleneVedtakMelding :
    HentLøsning<List<MidlertidigAleneVedtak>>,
    BehovMedLøsning<List<MidlertidigAleneVedtak>> {
    internal const val HentMidlertidigAleneVedtak = "HentMidlertidigAleneVedtak"
    private val behov = Behov(navn = HentMidlertidigAleneVedtak)

    override fun validate(packet: JsonMessage) {
        packet.interestedIn("vedtak")
    }

    override fun hentLøsning(packet: JsonMessage): List<MidlertidigAleneVedtak> {
        return (packet["vedtak"] as ArrayNode).map { it as ObjectNode }.map { MidlertidigAleneVedtak(
            periode = Periode(it["periode"].asText())
        )}
    }

    override fun behovMedLøsning(løsning: List<MidlertidigAleneVedtak>) =
        behov to mapOf(
            "vedtak" to løsning.map { mapOf(
                "periode" to it.periode.toString()
            )}
        )
}