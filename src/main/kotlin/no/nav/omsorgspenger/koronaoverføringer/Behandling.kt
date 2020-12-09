package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding

internal class Behandling(overføringen: OverføreKoronaOmsorgsdagerMelding.Behovet) {
    internal val periode = Perioder.behandlingsPeriode(
        periode = overføringen.periode,
        mottaksdato = overføringen.mottaksdato
    ) // TODO: Legge til vurdering på perioden.

    internal fun inneholderIkkeVerifiserbareVedtakOmUtvidetRett() = false // TODO
}