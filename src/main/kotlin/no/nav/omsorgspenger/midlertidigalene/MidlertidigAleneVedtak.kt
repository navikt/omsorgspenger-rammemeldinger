package no.nav.omsorgspenger.midlertidigalene

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode

internal data class MidlertidigAleneVedtak(
    val periode: Periode,
    val kilder: Set<Kilde>
)