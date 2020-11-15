package no.nav.omsorgspenger.behovssekvens

import java.time.ZonedDateTime

typealias BehovssekvensId = String
typealias BehovssekvensJSON = String

internal data class Behovssekvens(
    internal val behovssekvensId: BehovssekvensId,
    internal val behovssekvens: BehovssekvensJSON,
    internal val gjennomført: ZonedDateTime,
    internal val gjennomførtSteg: String
)