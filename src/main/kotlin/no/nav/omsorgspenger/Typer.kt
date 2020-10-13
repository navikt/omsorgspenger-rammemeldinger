package no.nav.omsorgspenger

typealias AktørId = String
typealias Identitetsnummer = String
typealias Saksnummer = String
typealias JournalpostId = String

internal data class Kilde(
    internal val id: String,
    internal val type: String
)