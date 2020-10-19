package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.overføringer.meldinger.SerDes.JacksonObjectMapper

internal object HentFordelingGirMeldingerMelding :
    BehovMedLøsning<List<FordelingGirMelding>>,
    HentLøsning<List<FordelingGirMelding>> {
    internal const val HentFordelingGirMeldinger = "HentFordelingGirMeldinger"
    private val behov = Behov(navn = HentFordelingGirMeldinger)
    private const val MeldingerKey = "@løsninger.$HentFordelingGirMeldinger.meldinger"

    override fun behovMedLøsning(løsning: List<FordelingGirMelding>) =
        behov to mapOf(
            "meldinger" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(MeldingerKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<FordelingGirMelding> {
        return JacksonObjectMapper.readValue(packet[MeldingerKey].toString())
    }
}