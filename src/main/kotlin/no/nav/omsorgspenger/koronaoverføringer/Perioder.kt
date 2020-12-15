package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.erFørEllerLik
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