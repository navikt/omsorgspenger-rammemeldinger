package no.nav.omsorgspenger.saksnummer

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer

internal class SaksnummerService(
    private val saksnummerRepository: SaksnummerRepository) {

    internal fun hentSaksnummer(identitetsnummer: Identitetsnummer) : Saksnummer? {
        // TODO: Her bør vi egentlig requeste omsorgspenger-sak om vi ikke finner
        // For de som potensielt har byttet identitetsnummer siden overføring ble gjort.
        return saksnummerRepository.hentSaksnummerFor(identitetsnummer)
    }

    internal fun hentSaksnummerIdentitetsnummerMapping(saksnummer: Set<Saksnummer>) = when (saksnummer.isEmpty()) {
        true -> mapOf()
        false -> saksnummerRepository.hentSisteMappingFor(saksnummer).entries.associate{(k,v)-> v to k}
    }

}