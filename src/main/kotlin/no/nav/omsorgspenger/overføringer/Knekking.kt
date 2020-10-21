package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal data class KnektPeriode(
    internal val periode: Periode,
    internal val starterGrunnet: Set<Knekkpunkt>,
    internal val slutterGrunnet: Set<Knekkpunkt>
)

internal enum class Knekkpunkt {
    Mottaksdato,
    NullstillingAvForbrukteDager,
    OmsorgenForEtBarnStarter,
    OmsorgenForEtBarnSlutter,
    FordelingGirStarter,
    FordelingGirSlutter,
    MidlertidigAleneStarter,
    MidlertidigAleneSlutter,
    OmsorgenForSlutter,
    OmsorgenForUtvidetRettSlutter
}

internal fun Grunnlag.knekk(overordnetPeriode: Periode) : List<KnektPeriode>  {
    this.overføreOmsorgsdager.overordnetPeriodeUtledetFraBarnMedUtvidetRett
    val knekkpunkt = mutableMapOf<LocalDate, MutableSet<Knekkpunkt>>().also {
        it.leggTil(
            periode = overordnetPeriode,
            fomKnekkpunkt = Knekkpunkt.Mottaksdato,
            tomKnekkpunkt = when (overføreOmsorgsdager.overordnetPeriodeUtledetFraBarnMedUtvidetRett) {
                true -> Knekkpunkt.OmsorgenForUtvidetRettSlutter
                false -> Knekkpunkt.OmsorgenForSlutter
            }
        )
    }

    if (overføreOmsorgsdager.omsorgsdagerTattUtIÅr > 0) {
        knekkpunkt.leggTil(
            dato = overføreOmsorgsdager.mottaksdato.førsteDagNesteÅr(),
            knekkpunkt = Knekkpunkt.NullstillingAvForbrukteDager
        )
    }

    overføreOmsorgsdager.barn.forEach { barn ->
        knekkpunkt.leggTil(
            periode = barn.omsorgenFor,
            fomKnekkpunkt = Knekkpunkt.OmsorgenForEtBarnStarter,
            tomKnekkpunkt = Knekkpunkt.OmsorgenForEtBarnSlutter
        )
    }

    fordelingGirMeldinger.forEach { fordelingGirMelding ->
        knekkpunkt.leggTil(
            periode = fordelingGirMelding.periode,
            fomKnekkpunkt = Knekkpunkt.FordelingGirStarter,
            tomKnekkpunkt = Knekkpunkt.FordelingGirSlutter
        )
    }

    midlertidigAleneVedtak.forEach { midlertidigAleneVedtak ->
        knekkpunkt.leggTil(
            periode = midlertidigAleneVedtak.periode,
            fomKnekkpunkt = Knekkpunkt.MidlertidigAleneStarter,
            tomKnekkpunkt = Knekkpunkt.MidlertidigAleneSlutter
        )
    }

    return knekkpunkt.keys.periodiser(
        overordnetPeriode = overordnetPeriode
    ).map { KnektPeriode(
        periode = it,
        starterGrunnet = knekkpunkt[it.fom] ?: error("Mangler knekkpunkt for ${it.fom}"),
        slutterGrunnet = it.tom.plusDays(1).let { førsteDagINestePeriode ->
            knekkpunkt[førsteDagINestePeriode] ?: error("Mangler knekkpunt for $førsteDagINestePeriode")
        }
    )}
}

private fun MutableMap<LocalDate, MutableSet<Knekkpunkt>>.leggTil(
    dato: LocalDate, knekkpunkt: Knekkpunkt) {
    put(dato, getOrDefault(dato, mutableSetOf()).also { it.add(knekkpunkt) })
}

private fun MutableMap<LocalDate, MutableSet<Knekkpunkt>>.leggTil(
    periode: Periode, fomKnekkpunkt: Knekkpunkt, tomKnekkpunkt: Knekkpunkt) {
    leggTil(periode.fom, fomKnekkpunkt)
    leggTil(periode.tom.plusDays(1), tomKnekkpunkt)
}