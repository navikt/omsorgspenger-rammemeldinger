package no.nav.omsorgspenger.personopplysninger

internal data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String) {
    override fun toString() = when (mellomnavn) {
        null -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }
}