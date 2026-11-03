package no.nav.omsorgspenger

typealias Identitetsnummer = String
typealias Saksnummer = String
typealias CorrelationId = String
typealias BehovssekvensId = String

internal data class Kilde(
    val id: String,
    val type: String) {
    internal companion object {
        internal fun internKilde(
            behovssekvensId: BehovssekvensId,
            type: String) = Kilde(
            id = behovssekvensId,
            type = "OmsorgspengerRammemeldinger[$type]"
        )
    }
}