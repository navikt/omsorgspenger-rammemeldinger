package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal data class Grunnlag(
    internal val overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
    internal val utvidetRettVedtak: List<UtvidetRettVedtak>,
    internal val fordelingGirMeldinger: List<FordelingGirMelding>,
    internal val midlertidigAleneVedtak: List<MidlertidigAleneVedtak>,
    internal val koronaOverføringer: List<SpleisetOverføringGitt>,
    internal val relasjoner: Set<VurderRelasjonerMelding.Relasjon>
)