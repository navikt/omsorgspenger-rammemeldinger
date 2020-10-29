package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.meldinger.SerDes.JacksonObjectMapper

internal object HentMidlertidigAleneVedtakMelding :
    BehovMedLøsning<List<MidlertidigAleneVedtak>>,
    HentLøsning<List<MidlertidigAleneVedtak>> {
    internal const val HentMidlertidigAleneVedtak = "HentMidlertidigAleneVedtak"
    private const val VedtakKey = "@løsninger.$HentMidlertidigAleneVedtak.vedtak"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: List<MidlertidigAleneVedtak>) =
        Behov(navn = HentMidlertidigAleneVedtak, input = behovInput) to mapOf(
            "vedtak" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(VedtakKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<MidlertidigAleneVedtak> {
        return JacksonObjectMapper.readValue(packet[VedtakKey].toString())
    }
}