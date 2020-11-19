package no.nav.omsorgspenger.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.leggTilLøsning

internal interface LeggTilBehov<BehovInput> {
    fun behov(behovInput: BehovInput) : Behov
}

internal interface HentBehov<Behovet> {
    fun validateBehov(packet: JsonMessage)
    fun hentBehov(packet: JsonMessage) : Behovet
}

internal interface HentLøsning<Løsning> {
    fun validateLøsning(packet: JsonMessage)
    fun hentLøsning(packet: JsonMessage): Løsning
}

internal interface LeggTilLøsning<Løsning> {
    fun løsning(løsning: Løsning) : Pair<String, Map<String,*>>
}

internal interface BehovMedLøsning<Løsning> {
    fun behovMedLøsning(behovInput: Map<String,*> = mapOf<String,Any>(), løsning: Løsning) : Pair<Behov, Map<String,*>>
}

internal fun JsonMessage.leggTilLøsningPar(pair: Pair<String, Map<String,*>>) =
    leggTilLøsning(behov = pair.first, løsning = pair.second)