package no.nav.omsorgspenger.fordelinger

import no.nav.omsorgspenger.Periode

internal data class FordelingGirMelding(
    internal val periode: Periode,
    internal val antallDager: Int
)