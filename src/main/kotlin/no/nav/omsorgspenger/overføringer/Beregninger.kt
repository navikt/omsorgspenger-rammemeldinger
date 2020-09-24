package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal object Beregninger {
    internal fun beregnOmsorgsdagerTilgjengeligForOverføring(
        grunnlag: Grunnlag,
        behandling: Behandling
    ) : Map<Periode, Int> {
        val beregnet = mutableMapOf<Periode, Int>()
        grunnlag.perioder().forEach { periode ->
            beregnet[periode] = beregn(grunnlag, periode)
        }
        return beregnet
    }

    private fun beregn(grunnlag: Grunnlag, periode: Periode) : Int {
        // TODO: Faktisk finne ut antall tilgjengelige dager..
        return 10
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

    fordelingGirMeldinger.forEach { fordelingGirMelding ->
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


internal fun Map<Periode, Int>.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(ønsketOmsorgsdagerÅOverføre: Int) =
    any { (_, omsorgsdagerTilgjengeligForOverføring) -> omsorgsdagerTilgjengeligForOverføring < ønsketOmsorgsdagerÅOverføre}