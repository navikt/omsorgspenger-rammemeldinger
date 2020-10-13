package no.nav.omsorgspenger.midlertidigalene

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode

internal data class MidlertidigAleneVedtak(
    internal val periode: Periode,
    internal val kilder: Set<Kilde>
)