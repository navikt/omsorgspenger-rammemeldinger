package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov

internal interface HentLøsning<Løsning> {
    fun validate(packet: JsonMessage)
    fun hentLøsning(packet: JsonMessage): Løsning
}

internal interface BehovMedLøsning<Løsning> {
    fun behovMedLøsning(løsning: Løsning) : Pair<Behov, Map<String,*>>
}