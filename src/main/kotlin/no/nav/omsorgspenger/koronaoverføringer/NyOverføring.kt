package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode

internal data class NyOverføring(
    val antallDager: Int,
    val periode: Periode
)