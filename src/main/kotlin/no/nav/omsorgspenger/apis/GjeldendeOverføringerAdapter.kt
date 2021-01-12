package no.nav.omsorgspenger.apis

import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer

internal interface GjeldendeOverføringerAdapter {
    fun hentGjeldendeOverføringer(saksnummer: Saksnummer) : Map<Saksnummer, GjeldendeOverføringer>
}