package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal data class Grunnlag(
    internal val overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Innhold,
    internal val utvidetRettVedtak: List<UtvidetRettVedtak>,
    internal val fordelingGirMeldinger: List<FordelingGirMelding>,
    internal val midlertidigAleneVedtak: List<MidlertidigAleneVedtak>
)