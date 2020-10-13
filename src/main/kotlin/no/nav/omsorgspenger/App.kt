package no.nav.omsorgspenger

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import java.net.URI

fun main() {
    RapidApplication.create(System.getenv()).apply {
        medAlleRivers()
    }.start()
}

internal fun RapidsConnection.medAlleRivers(
    env: Environment = System.getenv(),
    accessTokenClient: AccessTokenClient = ClientSecretAccessTokenClient(
        clientId = env.hentRequiredEnv("AZURE_CLIENT_ID"),
        clientSecret = env.hentRequiredEnv("AZURE_CLIENT_SECRET"),
        tokenEndpoint = URI(env.hentRequiredEnv("AZURE_TOKEN_ENDPOINT"))
    ),
    omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway = OmsorgspengerInfotrygdRammevedtakGateway(
        accessTokenClient = accessTokenClient,
        hentRammevedtakFraInfotrygdScopes = env.hentRequiredEnvSet("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES"),
        hentRammevedtakFraInfotrygdUrl = URI(env.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_URL"))
    ),
    infotrygdRammeService: InfotrygdRammeService = InfotrygdRammeService(
        omsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway
    ),
    fordelingService: FordelingService = FordelingService(
        infotrygdRammeService = infotrygdRammeService
    ),
    utvidetRettService: UtvidetRettService = UtvidetRettService(
        infotrygdRammeService = infotrygdRammeService
    ),
    midlertidigAleneService: MidlertidigAleneService = MidlertidigAleneService(
        infotrygdRammeService = infotrygdRammeService
    )) {

    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        fordelingService = fordelingService,
        utvidetRettService = utvidetRettService,
        midlertidigAleneService = midlertidigAleneService
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this
    )
}