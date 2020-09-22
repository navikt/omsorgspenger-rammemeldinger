package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal object BeregnTilgjengeligeOmsorgsdager {
    internal fun beregn(grunnlag: OverføreOmsorgsdagerGrunnlag) : Map<Periode, Resultat> {
        val beregnet = mutableMapOf<Periode, Resultat>()
        grunnlag.perioder().forEach { periode ->
            beregnet[periode] = beregn(grunnlag, periode)
        }
        return beregnet
    }

    private fun beregn(grunnlag: OverføreOmsorgsdagerGrunnlag, periode: Periode) : Resultat {
        return Resultat(dagerTilgjengelig = 10)
    }
}

private fun OverføreOmsorgsdagerGrunnlag.perioder() : List<Periode>  {
    val datoer = mutableListOf<LocalDate>()

    if (overføreOmsorgsdager.omsorgsdagerTattUtIÅr > 0) {
        datoer.add(overføreOmsorgsdager.mottaksdato.førsteDagNesteÅr())
    }

    overføreOmsorgsdager.barn.forEach { barn ->
        datoer.add(barn.omsorgenFor.fom)
        datoer.add(barn.omsorgenFor.tom.plusDays(1))
    }

    fordelingGirMeldinger.meldinger.forEach { fordelingGirMelding ->
        datoer.add(fordelingGirMelding.periode.fom)
        datoer.add(fordelingGirMelding.periode.tom.plusDays(1))
    }

    return datoer.periodiser(
        overordnetPeriode = overføreOmsorgsdager.overordnetPeriode
    )
}

internal data class Resultat(
    internal val hjemler: List<String> = listOf(),
    internal val dagerTilgjengelig: Int
)