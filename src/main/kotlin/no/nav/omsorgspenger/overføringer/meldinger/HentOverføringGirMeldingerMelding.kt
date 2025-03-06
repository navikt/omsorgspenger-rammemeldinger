package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.meldinger.SerDes

internal object HentOverføringGirMeldingerMelding :
    BehovMedLøsning<List<SpleisetOverføringGitt>>,
    HentLøsning<List<SpleisetOverføringGitt>> {
    internal const val HentOverføringGirMeldinger = "HentOverføringGirMeldinger"
    private const val MeldingerKey = "@løsninger.$HentOverføringGirMeldinger.meldinger"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: List<SpleisetOverføringGitt>) =
        Behov(navn = HentOverføringGirMeldinger, input = behovInput) to mapOf(
            "meldinger" to SerDes.JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(MeldingerKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<SpleisetOverføringGitt> {
        return SerDes.JacksonObjectMapper.readValue(packet[MeldingerKey].toString())
    }
}