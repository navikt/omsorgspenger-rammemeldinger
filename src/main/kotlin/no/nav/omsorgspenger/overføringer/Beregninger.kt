package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal object Beregninger {
    internal fun beregnOmsorgsdagerTilgjengeligForOverføring(
        grunnlag: Grunnlag,
        context: Context
    ) : Map<Periode, OmsorgsdagerTilgjengeligForOverføring> {
        val beregnet = mutableMapOf<Periode, OmsorgsdagerTilgjengeligForOverføring>()
        grunnlag.perioder().forEach { periode ->
            beregnet[periode] = beregn(grunnlag, periode)
        }
        return beregnet
    }

    private fun beregn(grunnlag: Grunnlag, periode: Periode) : OmsorgsdagerTilgjengeligForOverføring {
        return OmsorgsdagerTilgjengeligForOverføring(dagerTilgjengelig = 10)
    }
}

private fun Grunnlag.perioder() : List<Periode>  {
    val datoer = mutableListOf<LocalDate>()

    if (overføreOmsorgsdager.omsorgsdagerTattUtIÅr > 0) {
        datoer.add(overføreOmsorgsdager.mottaksdato.førsteDagNesteÅr())
    }

    overføreOmsorgsdager.barn.forEach { barn ->
        datoer.leggTilPeriode(barn.omsorgenFor)
    }

    fordelingGirMeldinger.meldinger.forEach { fordelingGirMelding ->
        datoer.leggTilPeriode(fordelingGirMelding.periode)
    }

    return datoer.periodiser(
        overordnetPeriode = overføreOmsorgsdager.overordnetPeriode
    )
}

private fun MutableList<LocalDate>.leggTilPeriode(periode: Periode) {
    add(periode.fom)
    add(periode.tom.plusDays(1))
}

internal data class OmsorgsdagerTilgjengeligForOverføring(
    internal val dagerTilgjengelig: Int
)

internal fun Map<Periode, OmsorgsdagerTilgjengeligForOverføring>.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(ønsketOmsorgsdagerÅOverføre: Int) =
    any { (_, omsorgsdagerTilgjengeligForOverføring) -> omsorgsdagerTilgjengeligForOverføring.dagerTilgjengelig < ønsketOmsorgsdagerÅOverføre}