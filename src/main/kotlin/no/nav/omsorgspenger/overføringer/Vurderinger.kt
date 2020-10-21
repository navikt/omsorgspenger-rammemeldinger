package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        behandling: Behandling) {
        val relasjon = grunnlag.overføreOmsorgsdager.relasjon
        val harBoddSammenMinstEtterÅr = grunnlag.overføreOmsorgsdager.harBoddSammentMinstEttÅr

        behandling.lovanvendelser.leggTil(
            periode = behandling.periode,
            lovhenvisning = BorINorge,
            anvendelse = when (grunnlag.overføreOmsorgsdager.borINorge) {
                true -> "Er bosatt i Norge."
                false -> "Må være bosatt i Norge for å overføre omsorgsdager.".also {
                    behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
                }
            }
        )

        behandling.lovanvendelser.leggTil(
            periode = behandling.periode,
            lovhenvisning = JobberINorge,
            anvendelse = when (grunnlag.overføreOmsorgsdager.jobberINorge) {
                true -> "Jobber i Norge."
                false -> "Må jobbe i Norge for å overføre omsorgsdager.".also {
                    behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
                }
            }
        )

        behandling.lovanvendelser.leggTil(
            periode = behandling.periode,
            lovhenvisning = EktefelleEllerSamboer,
            anvendelse = when {
                OverføreOmsorgsdagerMelding.Relasjon.NåværendeEktefelle == relasjon ->
                    "Overfører dager til ektefelle."
                OverføreOmsorgsdagerMelding.Relasjon.NåværendeSamboer == relasjon && harBoddSammenMinstEtterÅr == true ->
                    "Overfører dager til samboer som har vart i minst minst 12 måneder."
                else ->
                    "Må ha bodd sammen minst 12 måneder for å overføre dager til samboer.".also {
                        behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
                    }
            }
        )
    }

    internal fun vurderGrunnlag(
        grunnlag: Grunnlag,
        behandling: Behandling) : Grunnlag {
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak
        val alleBarn = grunnlag.overføreOmsorgsdager.barn
        val mottaksdato = grunnlag.overføreOmsorgsdager.mottaksdato

        val barnMedUtvidetRettSomIkkeKanVerifiseres = grunnlag
            .overføreOmsorgsdager
            .barn
            .filter { it.utvidetRett && !utvidetRettVedtak.inneholderRammevedtakFor(it, mottaksdato) }
            .onEach {
                val periode = when (it.omsorgenFor.fom.isBefore(mottaksdato)) {
                    true -> it.omsorgenFor.copy(fom = mottaksdato)
                    false -> it.omsorgenFor
                }
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = UtvidetRettForBarnet,
                    anvendelse = "Kunne ikke verifiser utvidet rett for barnet født ${it.fødselsdato}"
                )
            }.also {
                if (it.isNotEmpty()) {
                    behandling.leggTilKarakteristikk(Behandling.Karakteristikk.InneholderIkkeVerifiserbareVedtakOmUtvidetRett)
                }
            }

        return grunnlag.copy(
            overføreOmsorgsdager = grunnlag.overføreOmsorgsdager.copy(
                barn = alleBarn
                    .minus(barnMedUtvidetRettSomIkkeKanVerifiseres)
                    .plus(barnMedUtvidetRettSomIkkeKanVerifiseres.map { it.copy(utvidetRett = false) }),
            ).also {
                if (it.overordnetPeriodeUtledetFraBarnMedUtvidetRett) {
                    behandling.leggTilKarakteristikk(Behandling.Karakteristikk.VarighetPåOverføringUtledetFraBarnMedUtvidetRett)
                }
            }
        )
    }

    private fun List<UtvidetRettVedtak>.inneholderRammevedtakFor(barn: Barn, mottaksdato: LocalDate) =
        filter {
            it.barnetsFødselsdato.isEqual(barn.fødselsdato)
        }
        .also { if (it.size > 1) {
            logger.warn("Fant ${it.size} utvidet rett vedtak på barn født ${barn.fødselsdato}")
        }}
        .also { if (it.isNotEmpty()) {
            it.filter { vedtak ->
                vedtak.periode.tom != barn.omsorgenFor.tom
            }.also { uforventetTom -> if(uforventetTom.isNotEmpty()) {
                logger.warn("Fant ${uforventetTom.size} utvidet rett vedtak med 'tom' satt til noe annet enn ut året barnet fyller 18.")
            }}
            it.filter { vedtak ->
                vedtak.periode.fom.isAfter(mottaksdato)
            }.also { uforventetFom -> if(uforventetFom.isNotEmpty()) {
                logger.warn("Fant ${uforventetFom.size} utvidet rett vedtak med 'fom' satt til etter mottaksdato for overføringen.")
            }}
        }}
        .isNotEmpty()


    private val logger = LoggerFactory.getLogger(Vurderinger::class.java)

}