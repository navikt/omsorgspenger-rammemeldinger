package no.nav.omsorgspenger.overføringer

internal data class Grunnlag(
    internal val overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Innhold,
    internal val utvidetRettVedtak: HentUtvidetRettVedtakMelding.Innhold,
    internal val fordelingGirMeldinger: HentFordelingGirMeldingerMelding.Innhold
)