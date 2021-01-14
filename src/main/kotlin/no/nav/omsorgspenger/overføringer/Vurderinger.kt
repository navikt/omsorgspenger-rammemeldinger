package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.EktefelleEllerSamboer
import no.nav.omsorgspenger.lovverk.JobberINorge
import no.nav.omsorgspenger.lovverk.OmsorgenForBarnet
import no.nav.omsorgspenger.lovverk.UtvidetRettForBarnet
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtakVurderinger.inneholderRammevedtakFor

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        behandling: Behandling) {
        val relasjon = grunnlag.overføreOmsorgsdager.relasjon
        val harBoddSammenMinstEtterÅr = grunnlag.overføreOmsorgsdager.harBoddSammentMinstEttÅr

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
        relasjoner: Set<VurderRelasjonerMelding.Relasjon>,
        behandling: Behandling) : Grunnlag {
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak
        val alleBarn = grunnlag.overføreOmsorgsdager.barn
        val mottaksdato = grunnlag.overføreOmsorgsdager.mottaksdato

        val barnMedUtvidetRettSomIkkeKanVerifiseres = grunnlag
            .overføreOmsorgsdager
            .barn
            .filter { it.utvidetRett && !utvidetRettVedtak.inneholderRammevedtakFor(
                barnetsFødselsdato = it.fødselsdato,
                omsorgenForBarnet = it.omsorgenFor,
                mottaksdato = mottaksdato
            )}.onEach {
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

        val borSammenMed = relasjoner.filter { it.borSammen }.map { it.identitetsnummer }
        val barnSomBorNånAnnenstans = alleBarn
            .filterNot { it.identitetsnummer in borSammenMed }

        alleBarn.forEach { barn ->
            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = OmsorgenForBarnet,
                anvendelse = when (barn in barnSomBorNånAnnenstans) {
                    true -> "Bor ikke sammen med barnet født ${barn.fødselsdato}"
                    false -> "Bor sammen med barnet født ${barn.fødselsdato}"
                }
            )
        }

        return grunnlag.copy(
            overføreOmsorgsdager = grunnlag.overføreOmsorgsdager.copy(
                barn = alleBarn
                    .minus(barnSomBorNånAnnenstans)
                    .minus(barnMedUtvidetRettSomIkkeKanVerifiseres)
                    .plus(barnMedUtvidetRettSomIkkeKanVerifiseres.map { it.copy(utvidetRett = false) }),
            )
        )
    }
}