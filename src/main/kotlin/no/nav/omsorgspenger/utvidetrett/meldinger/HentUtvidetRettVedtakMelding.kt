package no.nav.omsorgspenger.utvidetrett.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.rivers.meldinger.SerDes.JacksonObjectMapper
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object HentUtvidetRettVedtakMelding :
    BehovMedLøsning<List<UtvidetRettVedtak>>,
    HentLøsning<List<UtvidetRettVedtak>> {
    internal const val HentUtvidetRettVedtak = "HentUtvidetRettVedtak"
    private const val VedtakKey = "@løsninger.$HentUtvidetRettVedtak.vedtak"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: List<UtvidetRettVedtak>) =
        Behov(navn = HentUtvidetRettVedtak, input = behovInput) to mapOf(
            "vedtak" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(VedtakKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<UtvidetRettVedtak> {
        return JacksonObjectMapper.readValue(packet[VedtakKey].toString())
    }
}