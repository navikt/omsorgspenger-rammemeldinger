package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.overføringer.meldinger.SerDes.JacksonObjectMapper
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object HentUtvidetRettVedtakMelding :
    BehovMedLøsning<List<UtvidetRettVedtak>>,
    HentLøsning<List<UtvidetRettVedtak>> {
    internal const val HentUtvidetRettVedtak = "HentUtvidetRettVedtak"
    private val behov = Behov(navn = HentUtvidetRettVedtak)
    private const val VedtakKey = "@løsninger.$HentUtvidetRettVedtak.vedtak"

    override fun behovMedLøsning(løsning: List<UtvidetRettVedtak>) =
        behov to mapOf(
            "vedtak" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(VedtakKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<UtvidetRettVedtak> {
        return JacksonObjectMapper.readValue(packet[VedtakKey].toString())
    }
}