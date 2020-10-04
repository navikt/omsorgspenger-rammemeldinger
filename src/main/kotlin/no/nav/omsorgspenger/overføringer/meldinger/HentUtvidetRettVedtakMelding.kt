package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object HentUtvidetRettVedtakMelding :
    BehovMedLøsning<List<UtvidetRettVedtak>> {
    internal const val HentUtvidetRettVedtak = "HentUtvidetRettVedtak"
    private val behov = Behov(navn = HentUtvidetRettVedtak)

    override fun behovMedLøsning(løsning: List<UtvidetRettVedtak>) =
        behov to mapOf(
            "vedtak" to løsning.map {
                mapOf(
                    "periode" to it.periode.toString(),
                    "barnetsFødselsdato" to it.barnetsFødselsdato.toString(),
                    "barnetsIdentitetsnummer" to it.barnetsIdentitetsnummer
                )
            }
        )
}