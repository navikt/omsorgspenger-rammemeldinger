package no.nav.omsorgspenger.behovssekvens

import no.nav.omsorgspenger.BehovssekvensId

internal typealias Behovssekvens = String

internal class BehovssekvensRepository {

    internal fun skalHåndtere(
        behovssekvensId: BehovssekvensId,
        steg: String) : Boolean {
        return true
    }

    internal fun harHåndtert(
        behovssekvensId: BehovssekvensId,
        behovssekvens: Behovssekvens,
        steg: String) {
    }
}