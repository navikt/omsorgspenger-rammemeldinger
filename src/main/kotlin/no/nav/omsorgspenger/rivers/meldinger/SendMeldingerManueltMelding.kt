package no.nav.omsorgspenger.rivers.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object SendMeldingerManueltMelding : LeggTilBehov<String> {

    private const val SendMeldingerManuelt = "SendMeldingerManuelt"

    override fun behov(behovInput: String) = Behov(
        navn = SendMeldingerManuelt,
        input = mapOf(
            "meldingerFor" to behovInput
        )
    )
}