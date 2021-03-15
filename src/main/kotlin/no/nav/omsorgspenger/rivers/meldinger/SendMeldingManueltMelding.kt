package no.nav.omsorgspenger.rivers.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal object SendMeldingManueltMelding : LeggTilBehov<String> {

    private const val SendMeldingManuelt = "SendMeldingManuelt"

    override fun behov(behovInput: String) = Behov(
        navn = SendMeldingManuelt,
        input = mapOf(
            "meldingstype" to behovInput
        )
    )
}