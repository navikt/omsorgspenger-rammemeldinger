package no.nav.omsorgspenger.fordelinger

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.Duration

internal data class FordelingGirMelding(
    internal val periode: Periode,
    internal val lengde: Duration,
    internal val kilder: Set<Kilde>
)