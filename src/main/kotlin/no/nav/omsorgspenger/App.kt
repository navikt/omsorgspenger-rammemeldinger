package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.StartOverføringAvOmsorgsdager
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService

fun main() {
    RapidApplication.create(System.getenv()).apply {
        medAlleRivers()
    }.start()
}

internal fun RapidsConnection.medAlleRivers() {
    StartOverføringAvOmsorgsdager(
        rapidsConnection = this,
        fordelingService = FordelingService(),
        utvidetRettService = UtvidetRettService()
    )
    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this
    )
}