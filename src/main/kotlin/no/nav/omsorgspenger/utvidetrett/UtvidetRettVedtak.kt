package no.nav.omsorgspenger.utvidetrett

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal data class UtvidetRettVedtak(
    val periode: Periode,
    val barnetsFødselsdato: LocalDate,
    val barnetsIdentitetsnummer: String? = null,
    val kilder: Set<Kilde>
)