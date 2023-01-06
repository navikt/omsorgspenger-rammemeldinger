package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.erFørEllerLik
import java.time.LocalDate

internal object Perioder {
    private val fraOgMed = LocalDate.now().withDayOfMonth(1).withMonth(1).withYear(2021)
    private val tilOgMed = LocalDate.now().withMonth(12).withDayOfMonth(31).withYear(2022)
    private val støttetPeriode = setOf(
        Periode(fom = fraOgMed, tom = tilOgMed.minusYears(1)),
        Periode(fom = fraOgMed.plusYears(1), tom = tilOgMed)
    )

    internal fun Periode.erStøttetPeriode() = støttetPeriode.contains(this)

    internal fun behandlingsPeriode(periode: Periode, mottaksdato: LocalDate) : Periode {
        require(periode.erStøttetPeriode()) {
            "Støtter ikke å behandle koronaoverføring for periode $periode"
        }
        require(mottaksdato.erFørEllerLik(tilOgMed)) {
            "Behandler ikke søknader mottatt $mottaksdato for perioden $periode"
        }

        return Periode(
            fom = when (mottaksdato.isBefore(fraOgMed)) {
                true -> fraOgMed
                false -> mottaksdato
            },
            tom = tilOgMed
        )
    }
}