package no.nav.omsorgspenger.fordelinger

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.Duration

internal data class FordelingGirMelding(
    val periode: Periode,
    val lengde: Duration,
    val kilder: Set<Kilde>
)