package no.nav.omsorgspenger.rivers

import no.nav.omsorgspenger.BehovssekvensId

internal typealias Behovssekvens = String

internal class BehovssekvensRepository {

    internal fun skalHåndtere(
        behovssekvensId: BehovssekvensId, status: String) : Boolean {
        return true
    }

    internal fun harHåndtert(
        behovssekvensId: BehovssekvensId,
        behovssekvens: Behovssekvens,
        status: String) {

    }
}