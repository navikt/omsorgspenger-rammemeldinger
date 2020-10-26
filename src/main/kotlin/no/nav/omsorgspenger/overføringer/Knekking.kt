package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal data class KnektPeriode(
    internal val periode: Periode,
    internal val starterGrunnet: List<Knekkpunkt>,
    internal val slutterGrunnet: List<Knekkpunkt>
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
    OmsorgenForMedUtvidetRettSlutter
}

internal fun Grunnlag.knekk(overordnetPeriode: Periode) : List<KnektPeriode>  {
    val boundary = Periode(
        fom = overordnetPeriode.fom,
        tom = overordnetPeriode.tom.plusDays(1)
    )

    val knekkpunkt = mutableMapOf<LocalDate, MutableList<Knekkpunkt>>().also {
        it.leggTil(
            periode = overordnetPeriode,
            fomKnekkpunkt = Knekkpunkt.Mottaksdato,
            tomKnekkpunkt = when (overføreOmsorgsdager.overordnetPeriodeUtledetFraBarnMedUtvidetRett) {
                true -> Knekkpunkt.OmsorgenForMedUtvidetRettSlutter
                false -> Knekkpunkt.OmsorgenForSlutter
            },
            boundary = boundary
        )
    }

    if (overføreOmsorgsdager.omsorgsdagerTattUtIÅr > 0) {
        knekkpunkt.leggTil(
            dato = overføreOmsorgsdager.mottaksdato.førsteDagNesteÅr(),
            knekkpunkt = Knekkpunkt.NullstillingAvForbrukteDager,
            boundary = boundary
        )
    }

    overføreOmsorgsdager.barn.forEach { barn ->
        knekkpunkt.leggTil(
            periode = barn.omsorgenFor,
            fomKnekkpunkt = Knekkpunkt.OmsorgenForEtBarnStarter,
            tomKnekkpunkt = Knekkpunkt.OmsorgenForEtBarnSlutter,
            boundary = boundary
        )
    }

    fordelingGirMeldinger.forEach { fordelingGirMelding ->
        knekkpunkt.leggTil(
            periode = fordelingGirMelding.periode,
            fomKnekkpunkt = Knekkpunkt.FordelingGirStarter,
            tomKnekkpunkt = Knekkpunkt.FordelingGirSlutter,
            boundary = boundary
        )
    }

    midlertidigAleneVedtak.forEach { midlertidigAleneVedtak ->
        knekkpunkt.leggTil(
            periode = midlertidigAleneVedtak.periode,
            fomKnekkpunkt = Knekkpunkt.MidlertidigAleneStarter,
            tomKnekkpunkt = Knekkpunkt.MidlertidigAleneSlutter,
            boundary = boundary
        )
    }

    return knekkpunkt.keys.periodiser(
        overordnetPeriode = overordnetPeriode
    ).map { KnektPeriode(
        periode = it,
        starterGrunnet = knekkpunkt.hentKnekkpunktFor(it.fom),
        slutterGrunnet = knekkpunkt.hentKnekkpunktFor(it.tom.plusDays(1))
    )}
}

private fun Map<LocalDate, List<Knekkpunkt>>.hentKnekkpunktFor(dato: LocalDate) =
    get(dato) ?: error("Mangler knekkpunkt for $dato")

private fun MutableMap<LocalDate, MutableList<Knekkpunkt>>.leggTil(
    dato: LocalDate,
    knekkpunkt: Knekkpunkt,
    boundary: Periode) {
    val benyttetDato = when {
        dato.isBefore(boundary.fom) -> boundary.fom
        dato.isAfter(boundary.tom) -> boundary.tom
        else -> dato
    }
    put(benyttetDato, getOrDefault(benyttetDato, mutableListOf()).also { it.add(knekkpunkt) })
}

private fun MutableMap<LocalDate, MutableList<Knekkpunkt>>.leggTil(
    periode: Periode,
    fomKnekkpunkt: Knekkpunkt,
    tomKnekkpunkt: Knekkpunkt,
    boundary: Periode) {
    leggTil(periode.fom, fomKnekkpunkt, boundary)
    leggTil(periode.tom.plusDays(1), tomKnekkpunkt, boundary)
}