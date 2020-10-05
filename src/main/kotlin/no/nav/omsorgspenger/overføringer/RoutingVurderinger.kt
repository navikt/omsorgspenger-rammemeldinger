package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import org.slf4j.LoggerFactory

internal object RoutingVurderinger {

    private val logger = LoggerFactory.getLogger(RoutingVurderinger::class.java)

    internal fun måBehandlesSomGosysJournalføringsoppgaver(
        behandling: Behandling,
        overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
        omsorgsdagerTilgjengeligForOverføring: Map<Periode, Int>
    ) : Boolean {
        if (!behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett()) return false

        return kotlin.runCatching {
            /**
             * Om behandlingen inneholder et ikke verifiservart vedtak om utvidet rett
             * vet vi at vi har avlått denne perioden. Men for å få gjennom så mange overføringer
             * som mulig innvilges overføringen kun ut året barnet fyller 12
             */
            val periodeUtenDagerTilgjengeligPåGrunnAvIkkeVerifiserbareVedtakOmUtvidetRett = Periode(
                fom = overføreOmsorgsdager.overordnetPeriode.tom.plusDays(1),
                tom = behandling.periode.tom
            )

            omsorgsdagerTilgjengeligForOverføring
                .minus(periodeUtenDagerTilgjengeligPåGrunnAvIkkeVerifiserbareVedtakOmUtvidetRett)
                .inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(
                    ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
                )
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                logger.error("Vurdering på måBehandlesSomGosysJournalføringsoppgaver feilet", throwable)
                true
            }
        )
    }
}