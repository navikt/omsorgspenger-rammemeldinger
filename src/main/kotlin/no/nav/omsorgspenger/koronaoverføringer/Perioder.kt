package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal object Perioder {
    private val fraOgMed = LocalDate.parse("2021-01-01")
    private val tilOgMed = LocalDate.parse("2021-12-31")
    private val støttetPeriode = Periode(fom = fraOgMed, tom = tilOgMed)

    internal fun Periode.erStøttetPeriode() = støttetPeriode == this

    internal fun behandlingsPeriode(periode: Periode, mottaksdato: LocalDate) : Periode {
        require(periode.erStøttetPeriode()) {
            "Støtter ikke å behandle koronaoverføring for periode $periode"
        }
        val niMånederTilbakeITid = mottaksdato.minusMonths(9)

        return Periode(
            fom = when (niMånederTilbakeITid.isAfter(fraOgMed)) {
                true -> niMånederTilbakeITid
                false -> fraOgMed
            },
            tom = tilOgMed
        )
    }
}