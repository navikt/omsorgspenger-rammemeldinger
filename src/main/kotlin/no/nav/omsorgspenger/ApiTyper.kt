package no.nav.omsorgspenger

import java.time.LocalDate

internal object ApiTyper {
    private val ElleveSiffer = "\\d{11}".toRegex()
    internal data class HentRammemeldingerRequest(
        val identitetsnummer: String, // TODO: Vailder
        val fom: LocalDate,
        val tom: LocalDate) {
        internal val periode = Periode(fom = fom, tom = tom)
        init {
            identitetsnummer.matches(ElleveSiffer)
        }

    }
}