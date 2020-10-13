package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding

internal object HentFordelingGirMeldingerMelding :
    BehovMedLøsning<List<FordelingGirMelding>> {

    internal const val HentFordelingGirMeldinger = "HentFordelingGirMeldinger"
    private val behov = Behov(navn = HentFordelingGirMeldinger)

    override fun behovMedLøsning(løsning: List<FordelingGirMelding>) =
        behov to mapOf(
            "meldinger" to løsning.map { mapOf(
                "periode" to it.periode.toString(),
                "lengde" to it.lengde.toString()
            )}
        )
}