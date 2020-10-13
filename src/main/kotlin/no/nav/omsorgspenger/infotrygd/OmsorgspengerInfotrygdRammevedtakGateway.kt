package no.nav.omsorgspenger.infotrygd

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import java.net.URI

internal class OmsorgspengerInfotrygdRammevedtakGateway(
    private val accessTokenClient: AccessTokenClient,
    private val hentRammevedtakFraInfotrygdScopes: Set<String>,
    private val hentRammevedtakFraInfotrygdUrl: URI
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal fun hent(identitetsnummer: Identitetsnummer, periode: Periode) : List<InfotrygdRamme> {
        return listOf()
    }
}