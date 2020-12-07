package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal data class Grunnlag(
    internal val overføringen: OverføreKoronaOmsorgsdagerMelding.Behovet,
    internal val utvidetRett: List<UtvidetRettVedtak>,
    internal val fordelinger: List<FordelingGirMelding>,
    internal val overføringer: List<SpleisetOverføringGitt>,
    internal val koronaoverføringer : List<GjeldendeOverføringGitt>) {
    internal companion object {
        internal fun Grunnlag.vurdert(behandling: Behandling) : Grunnlag {
            return this // TODO
        }
    }
}