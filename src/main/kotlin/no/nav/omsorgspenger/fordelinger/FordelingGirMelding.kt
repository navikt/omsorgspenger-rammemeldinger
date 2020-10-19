package no.nav.omsorgspenger.fordelinger

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.Duration

internal data class FordelingGirMelding(
    val periode: Periode,
    val lengde: Duration, // TODO: Serialieres feil.
    val kilder: Set<Kilde>
)