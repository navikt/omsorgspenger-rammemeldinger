package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
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
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule {
            install(Routing) {
                HealthRoute(healthService = applicationContext.healthService)
            }
        }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        fordelingService = applicationContext.fordelingService,
        utvidetRettService = applicationContext.utvidetRettService,
        midlertidigAleneService = applicationContext.midlertidigAleneService
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this
    )
    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })
}

internal class ApplicationContext(
    internal val env: Environment,
    internal val accessTokenClient: AccessTokenClient,
    internal val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway,
    internal val infotrygdRammeService: InfotrygdRammeService,
    internal val fordelingService: FordelingService,
    internal val utvidetRettService: UtvidetRettService,
    internal val midlertidigAleneService: MidlertidigAleneService,
    internal val healthService: HealthService) {

    internal fun start() {}
    internal fun stop() {}

    internal class Builder(
        internal var env: Environment? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
        internal var infotrygdRammeService: InfotrygdRammeService? = null,
        internal var fordelingService: FordelingService? = null,
        internal var utvidetRettService: UtvidetRettService? = null,
        internal var midlertidigAleneService: MidlertidigAleneService? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetAccessTokenClient = accessTokenClient?:ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
            )
            val benyttetOmsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway?:OmsorgspengerInfotrygdRammevedtakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnvSet("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES"),
                hentRammevedtakFraInfotrygdUrl = URI(benyttetEnv.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_URL"))
            )
            val benyttetInfotrygdRammeService = infotrygdRammeService?:InfotrygdRammeService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )

            return ApplicationContext(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway,
                infotrygdRammeService = benyttetInfotrygdRammeService,
                fordelingService = fordelingService ?: FordelingService(
                    infotrygdRammeService = benyttetInfotrygdRammeService
                ),
                utvidetRettService = utvidetRettService ?: UtvidetRettService(
                    infotrygdRammeService = benyttetInfotrygdRammeService
                ),
                midlertidigAleneService = midlertidigAleneService ?: MidlertidigAleneService(
                    infotrygdRammeService = benyttetInfotrygdRammeService
                ),
                healthService = HealthService(healthChecks = setOf(
                    benyttetOmsorgspengerInfotrygdRammevedtakGateway
                ))
            )
        }
    }
}