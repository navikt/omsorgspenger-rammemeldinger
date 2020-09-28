package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        behandling: Behandling) {
        val overordnetPeriode = behandling.periode

        if (!grunnlag.overføreOmsorgsdager.barn.any { it.aleneOmOmsorgen }) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for minst ett barn for å kunne overføre omsorgsdager."
            )
            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        if (!grunnlag.overføreOmsorgsdager.borINorge) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
                anvendelse = "Må være bosatt i Norge for å overføre omsorgsdager."
            )
            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        if (!grunnlag.overføreOmsorgsdager.jobberINorge) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
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
            .filter { it.utvidetRett && !utvidetRettVedtak.inneholderRammevedtakFor(it) }
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
                barn = alleBarn.minus(barnMedUtvidetRettSomIkkeKanVerifiseres),
            )
        )
    }
}

private fun List<UtvidetRettVedtak>.inneholderRammevedtakFor(barn: Barn) =
    any { it.barnetsFødselsdato.isEqual(barn.fødselsdato) }