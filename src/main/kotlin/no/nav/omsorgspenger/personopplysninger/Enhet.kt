package no.nav.omsorgspenger.personopplysninger

internal data class Enhet(
    internal val enhetstype: Enhetstype,
    internal val enhetsnummer: String) {
    internal val skjermet = enhetstype == Enhetstype.SKJERMET
}

internal enum class Enhetstype {
    VANLIG,
    SKJERMET;

    internal companion object {
        internal fun fromDTO(dto: String) = kotlin.runCatching { valueOf(dto) }.fold(
            onSuccess = {it},
            onFailure = {SKJERMET}
        )
    }
}