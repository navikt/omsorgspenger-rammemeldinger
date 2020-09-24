package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        behandling: Behandling) {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode

        if (!grunnlag.overføreOmsorgsdager.barn.any { it.aleneOmOmsorgen }) {
            behandling.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for mist ett barn for å kunne overføre omsorgsdager."
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

    internal fun vurderOmsorgenFor(
        grunnlag: Grunnlag,
        behandling: Behandling) : Grunnlag {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak

        val barnMedOmsorgenFor = grunnlag
            .overføreOmsorgsdager
            .barn
            .onEach {
                if (it.aleneOmOmsorgen) {
                    behandling.lovanvendelser.leggTil(
                        periode = overordnetPeriode,
                        lovhenvisning = AleneOmOmsorgenForBarnet,
                        anvendelse = "Er alene om omsorgen for barnet født ${it.fødselsdato}."
                    )
                }
            }
            .filter {
                when (it.utvidetRett) {
                    true -> {
                        val utvidetRettVerifisert = utvidetRettVedtak.inneholderRammevedtakFor(it)
                        if (!utvidetRettVerifisert) {
                            behandling.leggTilKarakteristikk(Behandling.Karakteristikk.InneholderIkkeVerifiserbareVedtakOmUtvidetRett)
                        } else {
                            behandling.lovanvendelser.leggTil(
                                periode = overordnetPeriode,
                                lovhenvisning = UtvidetRettForBarnet,
                                anvendelse = "Har utvidet rett for barnet født ${it.fødselsdato}."
                            )
                        }
                        utvidetRettVerifisert
                    }
                    false -> true
                }
            }

        return grunnlag.copy(
            overføreOmsorgsdager = grunnlag.overføreOmsorgsdager.copy(
                barn = barnMedOmsorgenFor,
            )
        )
    }
}

private fun List<UtvidetRettVedtak>.inneholderRammevedtakFor(barn: Barn) =
    any { it.barnetsFødselsdato.isEqual(barn.fødselsdato) }