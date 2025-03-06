package no.nav.omsorgspenger.fordelinger.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.rivers.meldinger.SerDes.JacksonObjectMapper
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning

internal object HentFordelingGirMeldingerMelding :
    BehovMedLøsning<List<FordelingGirMelding>>,
    HentLøsning<List<FordelingGirMelding>> {
    internal const val HentFordelingGirMeldinger = "HentFordelingGirMeldinger"
    private const val MeldingerKey = "@løsninger.$HentFordelingGirMeldinger.meldinger"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: List<FordelingGirMelding>) =
        Behov(navn = HentFordelingGirMeldinger, input = behovInput) to mapOf(
            "meldinger" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(MeldingerKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<FordelingGirMelding> {
        return JacksonObjectMapper.readValue(packet[MeldingerKey].toString())
    }
}