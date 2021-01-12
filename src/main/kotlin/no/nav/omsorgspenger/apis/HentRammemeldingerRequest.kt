package no.nav.omsorgspenger.apis

import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal data class HentRammemeldingerRequest(
    val identitetsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate) {
    internal val periode = Periode(fom = fom, tom = tom)
    init {
        identitetsnummer.matches(ElleveSiffer)
    }
    private companion object {
        private val ElleveSiffer = "\\d{11}".toRegex()
    }
}