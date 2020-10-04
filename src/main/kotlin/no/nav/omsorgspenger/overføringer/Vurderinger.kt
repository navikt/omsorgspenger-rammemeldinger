package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        behandling: Behandling) {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode

        if (!grunnlag.overføreOmsorgsdager.borINorge) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = BorINorge,
                anvendelse = "Må være bosatt i Norge for å overføre omsorgsdager."
            )
            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        if (!grunnlag.overføreOmsorgsdager.jobberINorge) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = JobberINorge,
                anvendelse = "Må jobbe i Norge for å overføre omsorgsdager."
            )
            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        // TODO : samboer minst 1 år..

        if (grunnlag.overføreOmsorgsdager.sendtPerBrev) {
            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.MåBesvaresPerBrev)
        }
    }

    internal fun vurderGrunnlag(
        grunnlag: Grunnlag,
        behandling: Behandling) : Grunnlag {
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak
        val alleBarn = grunnlag.overføreOmsorgsdager.barn

        val barnMedUtvidetRettSomIkkeKanVerifiseres = grunnlag
            .overføreOmsorgsdager
            .barn
            .filter { it.utvidetRett && !utvidetRettVedtak.inneholderRammevedtakFor(it, grunnlag.overføreOmsorgsdager.mottaksdato) }
            .onEach {
                behandling.lovanvendelser.leggTil(
                    periode = it.omsorgenFor,
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
            )
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