package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.meldinger.SerDes.JacksonObjectMapper

internal object HentKoronaOverføringGirMeldingerMelding :
    BehovMedLøsning<List<SpleisetOverføringGitt>>,
    HentLøsning<List<SpleisetOverføringGitt>> {
    internal const val HentKoronaOverføringGirMeldinger = "HentKoronaOverføringGirMeldinger"
    private const val MeldingerKey = "@løsninger.$HentKoronaOverføringGirMeldinger.meldinger"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: List<SpleisetOverføringGitt>) =
        Behov(navn = HentKoronaOverføringGirMeldinger, input = behovInput) to mapOf(
            "meldinger" to JacksonObjectMapper.convertValue<List<*>>(løsning)
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(MeldingerKey)
    }

    override fun hentLøsning(packet: JsonMessage): List<SpleisetOverføringGitt> {
        return JacksonObjectMapper.readValue(packet[MeldingerKey].toString())
    }
}