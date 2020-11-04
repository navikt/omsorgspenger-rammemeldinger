package no.nav.omsorgspenger.saksnummer

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer

internal class SaksnummerRepository {

    private val map = mutableMapOf<Identitetsnummer, Saksnummer>()

    internal fun lagreMapping(mapping: Map<Identitetsnummer, Saksnummer>) {
        map.putAll(mapping)
    }

    internal fun hentSisteMappingFor(saksnummer: Set<Saksnummer>) : Map<Identitetsnummer, Saksnummer> {
        return map.filterValues { saksnummer.contains(it) }
    }
}

internal fun Map<Saksnummer, Identitetsnummer>.identitetsnummer() = keys