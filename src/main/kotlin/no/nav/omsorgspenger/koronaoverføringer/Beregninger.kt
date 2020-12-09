package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import org.slf4j.LoggerFactory

internal object Beregninger {
    private val logger = LoggerFactory.getLogger(Beregninger::class.java)

    private const val DagerMedGrunnrettOppTilToBarn = 20
    private const val DagerMedGrunnrettTreEllerFlerBarn = 30

    private const val DagerMedAleneOmsorgOppTilToBarn = 20
    private const val DagerMedAleneOmsorgTreEllerFlerBarn = 30

    private const val DagerMedUtvidetRettPerBarn = 20
    private const val DagerMedUtvidetRettOgAleneOmsorgPerBarn = 40

    internal fun beregnDagerTilgjengeligForOverføring(
        behandling: Behandling,
        grunnlag: Grunnlag) : Int {

        val antallOmsorgsdager = beregnOmsorgsdager(
            barn = grunnlag.overføringen.barn,
            periode = behandling.periode
        ).antallOmsorgsdager

        val antallDagerFordelt = grunnlag.fordelinger
            .filter { fordelinger -> fordelinger.periode.overlapperMedMinstEnDag(behandling.periode) }
            .let { overlappendeFordelinger -> when {
                overlappendeFordelinger.isEmpty() -> 0
                overlappendeFordelinger.size == 1 -> overlappendeFordelinger.first().lengde.antallDager()
                else -> logger.warn("Fant ${overlappendeFordelinger.size} overlappende fordelinger, bruker lengden på den lengste").let {
                    overlappendeFordelinger.maxByOrNull { it.lengde }!!.lengde.antallDager()
                }
            }}

        val antallDagerOverført = grunnlag.overføringer
            .filter { overføringer -> overføringer.periode().overlapperMedMinstEnDag(behandling.periode) }
            .let { overlappendeOverføringer -> when {
                overlappendeOverføringer.isEmpty() -> 0
                overlappendeOverføringer.size == 1 -> overlappendeOverføringer.first().lengde.antallDager()
                else -> logger.warn("Fant ${overlappendeOverføringer.size} overlappende overføringer, bruker lengde på den lengste").let {
                    overlappendeOverføringer.maxByOrNull { it.lengde }!!.lengde.antallDager()
                }
            }}

        val antallDagerKoronaoverført = grunnlag.koronaoverføringer
            .filter { koronaOverføringer -> koronaOverføringer.periode.overlapperMedMinstEnDag(behandling.periode) }
            .sumBy { it.antallDager }

        val antallDagerTilgjengelig = antallOmsorgsdager -
            antallDagerFordelt -
            antallDagerOverført -
            antallDagerKoronaoverført -
            grunnlag.overføringen.omsorgsdagerTattUtIÅr

        val antallDagerTilgjengeligForOverføring = when (antallDagerTilgjengelig < 0) {
            true -> 0
            else -> antallDagerTilgjengelig
        }

        return antallDagerTilgjengeligForOverføring
    }

    private fun beregnOmsorgsdager(barn: List<OverføreKoronaOmsorgsdagerMelding.Barn>, periode: Periode): OmsorgsdagerResultat {
        val barnMedOmsorgenFor = barn.filter { it.omsorgenFor.inneholder(periode) }
        val barnMedAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.aleneOmOmsorgen }
        val barnMedKunUtvidetRett = barnMedOmsorgenFor.filter { it.utvidetRett && !it.aleneOmOmsorgen }
        val barnMedUtvidetRettOgAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.utvidetRett && it.aleneOmOmsorgen }

        val grunnrett = when (barnMedOmsorgenFor.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            in 1..2 -> DagerForBarn(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettOppTilToBarn)
            else -> DagerForBarn(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettTreEllerFlerBarn)
        }

        val aleneOmsorg = when (barnMedAleneOmOmsorgen.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            in 1..2 -> DagerForBarn(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgOppTilToBarn)
            else -> DagerForBarn(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgTreEllerFlerBarn)
        }

        val utvidetRett = when (barnMedKunUtvidetRett.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            else -> DagerForBarn(antallBarn = barnMedKunUtvidetRett.size, antallDager = DagerMedUtvidetRettPerBarn * barnMedKunUtvidetRett.size)
        }

        val aleneomsorgOgUtvidetRett = when (barnMedUtvidetRettOgAleneOmOmsorgen.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            else -> DagerForBarn(
                antallBarn = barnMedUtvidetRettOgAleneOmOmsorgen.size,
                antallDager = DagerMedUtvidetRettOgAleneOmsorgPerBarn * barnMedUtvidetRettOgAleneOmOmsorgen.size
            )
        }

        return OmsorgsdagerResultat(
            grunnrettsdager = grunnrett,
            aleneomsorgsdager = aleneOmsorg,
            utvidetRettDager = utvidetRett,
            aleneomsorgOgUtvidetRettDager = aleneomsorgOgUtvidetRett
        )
    }

    private data class DagerForBarn(
        val antallBarn: Int,
        val antallDager: Int
    )

    private data class OmsorgsdagerResultat(
        val grunnrettsdager: DagerForBarn,
        val aleneomsorgsdager: DagerForBarn,
        val utvidetRettDager: DagerForBarn,
        val aleneomsorgOgUtvidetRettDager: DagerForBarn) {
        val antallOmsorgsdager = grunnrettsdager.antallDager +
            aleneomsorgsdager.antallDager +
            utvidetRettDager.antallDager +
            aleneomsorgOgUtvidetRettDager.antallDager
    }

    private fun SpleisetOverføringGitt.periode() = Periode(fom = gyldigFraOgMed, tom = gyldigTilOgMed)
}

