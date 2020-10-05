package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak

internal object HentMidlertidigAleneVedtakMelding :
    BehovMedLøsning<List<MidlertidigAleneVedtak>> {
    internal const val HentMidlertidigAleneVedtak = "HentMidlertidigAleneVedtak"
    private val behov = Behov(navn = HentMidlertidigAleneVedtak)

    override fun behovMedLøsning(løsning: List<MidlertidigAleneVedtak>) =
        behov to mapOf(
            "vedtak" to løsning.map { mapOf(
                "periode" to it.periode.toString()
            )}
        )
}