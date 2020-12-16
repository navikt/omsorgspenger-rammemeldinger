package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding

internal class Behandling(behovet: OverføreKoronaOmsorgsdagerMelding.Behovet) {
    internal var inneholderIkkeVerifiserbareVedtakOmUtvidetRett = false

    internal val periode = Perioder.behandlingsPeriode(
        periode = behovet.periode,
        mottaksdato = behovet.mottaksdato
    )
}