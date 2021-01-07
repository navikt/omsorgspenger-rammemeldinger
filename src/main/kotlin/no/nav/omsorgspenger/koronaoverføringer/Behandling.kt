package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.lovverk.Lovanvendelser

internal class Behandling(behovet: OverføreKoronaOmsorgsdagerMelding.Behovet) {
    internal var inneholderIkkeVerifiserbareVedtakOmUtvidetRett = false
    internal var gjennomførtOverføringer = false

    internal val lovanvendelser = Lovanvendelser()

    internal val periode = Perioder.behandlingsPeriode(
        periode = behovet.periode,
        mottaksdato = behovet.mottaksdato
    )
}