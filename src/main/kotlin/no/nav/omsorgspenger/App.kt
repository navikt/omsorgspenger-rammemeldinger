package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.FerdigstillOverføringAvOmsorgsdager
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService

fun main() {
    RapidApplication.create(System.getenv()).apply {
        medAlleRivers()
    }.start()
}

internal fun RapidsConnection.medAlleRivers() {
    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        fordelingService = FordelingService(),
        utvidetRettService = UtvidetRettService()
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this
    )
    FerdigstillOverføringAvOmsorgsdager(
        rapidsConnection = this
    )
}