package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.førsteDagNesteÅr
import no.nav.omsorgspenger.periodiser
import java.time.LocalDate

internal object Beregninger {
    private const val DagerMaksForOverføring = 10

    private const val DagerMedGrunnrettOppTilToBarn = 10
    private const val DagerMedGrunnrettTreEllerFlerBarn = 15

    private const val DagerMedAleneOmsorgOppTilToBarn = 10
    private const val DagerMedAleneOmsorgTreEllerFlerBarn = 15

    private const val DagerMedUtvidetRettPerBarn = 10
    private const val DagerMedUtvidetRettOgAleneOmsorgPerBarn = 20

    internal fun beregnOmsorgsdagerTilgjengeligForOverføring(
        grunnlag: Grunnlag,
        behandling: Behandling
    ) : Map<Periode, Int> {
        val beregnet = mutableMapOf<Periode, Int>()
        grunnlag.perioder(behandling.periode).forEach { periode ->
            beregnet[periode] = beregnPeriode(grunnlag, periode, behandling)
        }
        return beregnet
    }

    private fun beregnPeriode(grunnlag: Grunnlag, periode: Periode, behandling: Behandling) : Int {
        val barnMedOmsorgenFor = grunnlag.overføreOmsorgsdager.barn.filter { it.omsorgenFor.inneholder(periode) }
        val fordelingGirMeldinger = grunnlag.fordelingGirMeldinger.filter { it.periode.inneholder(periode) }

        // TODO: Fjern?
        if (barnMedOmsorgenFor.isEmpty()) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = GrunnrettOppTilToBarn,
                anvendelse = "Ikke omsorgen for noen barn"
            )
            return 0
        }

        when (barnMedOmsorgenFor.size) {
            in 1..2 -> {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = GrunnrettOppTilToBarn,
                    anvendelse = "Omsorgen for ${barnMedOmsorgenFor.size} barn"
                )
            }
            else -> {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = GrunnrettTreEllerFlerBarn,
                    anvendelse = "Omsorgen for ${barnMedOmsorgenFor.size} barn"
                )
            }
        }

        val barnMedAleneOmOmsorgen = barnMedOmsorgenFor.filter {  it.aleneOmOmsorgen }.also { if (it.isNotEmpty()) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Alene om omsorgen for ${it.size} barn"
            )
        }}
        val barnMedUtvidetRett = barnMedOmsorgenFor.filter { it.utvidetRett }.also { if (it.isNotEmpty()) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettForBarnet,
                anvendelse = "Utvidet rett for ${it.size} barn"
            )
        }}
        val barnMedUtvidetRettOgAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.utvidetRett && it.aleneOmOmsorgen }.also { if(it.isNotEmpty())  {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettOgAleneOmOmsorgenForBarnet,
                anvendelse = "Utvidet rett og alene om omsorgen for ${it.size} barn"
            )
        }}

        val omsorgsdager = beregnOmsorgsdager(
            antallBarnMedOmsorgenFor = barnMedOmsorgenFor.size,
            antallBarnMedAleneOmOmsorgen = barnMedAleneOmOmsorgen.size,
            antallBarnMedUtvidetRett = barnMedUtvidetRett.size,
            antallBarnMedUtvidetRettOgAleneOmOmsorgen = barnMedUtvidetRettOgAleneOmOmsorgen.size
        ).also {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = AntallOmsorgsdager,
                anvendelse = "Har $it omsorgsdager"
            )
        }

        val dagerTattUt = when (grunnlag.overføreOmsorgsdager.mottaksdato.year == periode.tom.year) {
            true -> grunnlag.overføreOmsorgsdager.omsorgsdagerTattUtIÅr.also { if (it > 0) {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = AlleredeForbrukteDager,
                    anvendelse = "Allerede tatt ut $it dager"
                )
            }}
            false -> 0
        }

        val fordeltBort = fordelingGirMeldinger.sumBy { it.antallDager }.also { if (it > 0) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = FordeltBortOmsorgsdager,
                anvendelse = "Har fordelt bort $it dager"
            )
        }}

        val tilgjengeligeDager = omsorgsdager - dagerTattUt - fordeltBort

        return when (tilgjengeligeDager > DagerMaksForOverføring) {
            true -> DagerMaksForOverføring
            false -> tilgjengeligeDager
        }
    }

    private fun beregnOmsorgsdager(
        antallBarnMedOmsorgenFor: Int,
        antallBarnMedAleneOmOmsorgen: Int,
        antallBarnMedUtvidetRett: Int,
        antallBarnMedUtvidetRettOgAleneOmOmsorgen : Int
    ) : Int {
        val grunnrett = when (antallBarnMedOmsorgenFor) {
            0 -> return 0
            in 1..2 -> DagerMedGrunnrettOppTilToBarn
            else -> DagerMedGrunnrettTreEllerFlerBarn
        }

        val aleneOmsorg = when (antallBarnMedAleneOmOmsorgen) {
            0 -> 0
            in 1..2 -> DagerMedAleneOmsorgOppTilToBarn
            else -> DagerMedAleneOmsorgTreEllerFlerBarn
        }

        val utvidetRett = when (antallBarnMedUtvidetRett) {
            0 -> 0
            else -> DagerMedUtvidetRettPerBarn * antallBarnMedUtvidetRett
        }

        val utvidetRettOgAleneOmsorg = when (antallBarnMedUtvidetRettOgAleneOmOmsorgen) {
            0 -> 0
            else -> DagerMedUtvidetRettOgAleneOmsorgPerBarn * antallBarnMedUtvidetRettOgAleneOmOmsorgen
        }

        return grunnrett + aleneOmsorg + utvidetRett + utvidetRettOgAleneOmsorg
    }
}

private fun Grunnlag.perioder(overordnetPeriode: Periode) : List<Periode>  {
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
        overordnetPeriode = overordnetPeriode
    )
}

private fun MutableList<LocalDate>.leggTilPeriode(periode: Periode) {
    add(periode.fom)
    add(periode.tom.plusDays(1))
}


internal fun Map<Periode, Int>.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(ønsketOmsorgsdagerÅOverføre: Int) =
    any { (_, omsorgsdagerTilgjengeligForOverføring) -> omsorgsdagerTilgjengeligForOverføring < ønsketOmsorgsdagerÅOverføre}

internal fun Map<Periode, Int>.ingenDagerTilgjengeligForOverføring() =
    all { it.value == 0 }