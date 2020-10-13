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
        AppBuilder().build(this)
    }.start()
}

internal class AppBuilder(
    internal var env: Environment? = null,
    internal var accessTokenClient: AccessTokenClient? = null,
    internal var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
    internal var infotrygdRammeService: InfotrygdRammeService? = null,
    internal var fordelingService: FordelingService? = null,
    internal var utvidetRettService: UtvidetRettService? = null,
    internal var midlertidigAleneService: MidlertidigAleneService? = null){
    internal fun build(rapidsConnection: RapidsConnection) : RapidsConnection {
        val benyttetEnv = env?:System.getenv()
        val benyttetAccessTokenClient = accessTokenClient?:ClientSecretAccessTokenClient(
            clientId = benyttetEnv.hentRequiredEnv("AZURE_CLIENT_ID"),
            clientSecret = benyttetEnv.hentRequiredEnv("AZURE_CLIENT_SECRET"),
            tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_TOKEN_ENDPOINT"))
        )
        val benyttetOmsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway?:OmsorgspengerInfotrygdRammevedtakGateway(
            accessTokenClient = benyttetAccessTokenClient,
            hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnvSet("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES"),
            hentRammevedtakFraInfotrygdUrl = URI(benyttetEnv.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_URL"))
        )
        val benyttetInfotrygdRammeService = infotrygdRammeService?:InfotrygdRammeService(
            omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
        )
        BehandleOverføringAvOmsorgsdager(
            rapidsConnection = rapidsConnection,
            fordelingService = fordelingService?: FordelingService(
                infotrygdRammeService = benyttetInfotrygdRammeService
            ),
            utvidetRettService = utvidetRettService?: UtvidetRettService(
                infotrygdRammeService = benyttetInfotrygdRammeService
            ),
            midlertidigAleneService = midlertidigAleneService?:MidlertidigAleneService(
                infotrygdRammeService = benyttetInfotrygdRammeService
            )
        )
        PubliserOverføringAvOmsorgsdager(
            rapidsConnection = rapidsConnection
        )
        return rapidsConnection
    }
}