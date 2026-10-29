package no.nav.omsorgspenger

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenRepository
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerService
import no.nav.omsorgspenger.koronaoverføringer.db.KoronaoverføringRepository
import no.nav.omsorgspenger.overføringer.GjennomførOverføringService
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.overføringer.apis.TilgangsstyringRestClient
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import java.net.URI
import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val accessTokenClient: AccessTokenClient,
    internal val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway,
    internal val infotrygdRammeService: InfotrygdRammeService,
    internal val gjennomførOverføringService: GjennomførOverføringService,
    internal val overføringRepository: OverføringRepository,
    internal val spleisetOverføringerService: SpleisetOverføringerService,
    internal val aleneOmOmsorgenRepository: AleneOmOmsorgenRepository,
    internal val saksnummerRepository: SaksnummerRepository,
    internal val saksnummerService: SaksnummerService,
    internal val tilgangsstyringRestClient: TilgangsstyringRestClient,
    internal val dataSource: DataSource,
    internal val healthService: HealthService,
    internal val koronaoverføringRepository: KoronaoverføringRepository,
    internal val spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService
) {

    internal fun start() {
        dataSource.migrate()
    }

    internal class Builder(
        internal var env: Environment? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
        internal var infotrygdRammeService: InfotrygdRammeService? = null,
        internal var gjennomførOverføringService: GjennomførOverføringService? = null,
        internal var overføringRepository: OverføringRepository? = null,
        internal var spleisetOverføringerService: SpleisetOverføringerService? = null,
        internal var tilgangsstyringRestClient: TilgangsstyringRestClient? = null,
        internal var aleneOmOmsorgenRepository: AleneOmOmsorgenRepository? = null,
        internal var saksnummerRepository: SaksnummerRepository? = null,
        internal var saksnummerService: SaksnummerService? = null,
        internal var dataSource: DataSource? = null,
        internal var koronaoverføringRepository: KoronaoverføringRepository? = null,
        internal var spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService? = null
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetAccessTokenClient = accessTokenClient ?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
                authenticationMode = ClientSecretAccessTokenClient.AuthenticationMode.POST
            )
            val benyttetOmsorgspengerInfotrygdRammevedtakGateway =
                omsorgspengerInfotrygdRammevedtakGateway ?: OmsorgspengerInfotrygdRammevedtakGateway(
                    accessTokenClient = benyttetAccessTokenClient,
                    hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_SCOPES")
                        .csvTilSet(),
                    omsorgspengerInfotrygdRammevedtakBaseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL"))
                )
            val benyttetInfotrygdRammeService = infotrygdRammeService ?: InfotrygdRammeService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )

            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()

            val benyttetOverføringRepository = overføringRepository ?: OverføringRepository(
                dataSource = benyttetDataSource
            )

            val benyttetAleneOmOmsorgenRepository = aleneOmOmsorgenRepository ?: AleneOmOmsorgenRepository(
                dataSource = benyttetDataSource
            )

            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(
                dataSource = benyttetDataSource
            )

            val benyttetSaksnummerService = saksnummerService ?: SaksnummerService(
                saksnummerRepository = benyttetSaksnummerRepository
            )

            val benyttetTilgangsstyringRestClient = tilgangsstyringRestClient ?: TilgangsstyringRestClient(
                httpClient = HttpClient {
                    install(ContentNegotiation) {
                        jackson()
                    }
                },
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient
            )

            val benyttetKoronaoverføringRepository = koronaoverføringRepository ?: KoronaoverføringRepository(
                dataSource = benyttetDataSource
            )

            return ApplicationContext(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway,
                infotrygdRammeService = benyttetInfotrygdRammeService,
                gjennomførOverføringService = gjennomførOverføringService ?: GjennomførOverføringService(
                    overføringRepository = benyttetOverføringRepository
                ),
                aleneOmOmsorgenRepository = benyttetAleneOmOmsorgenRepository,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetOmsorgspengerInfotrygdRammevedtakGateway
                    )
                ),
                dataSource = benyttetDataSource,
                overføringRepository = benyttetOverføringRepository,
                spleisetOverføringerService = spleisetOverføringerService ?: SpleisetOverføringerService(
                    infotrygdRammeService = benyttetInfotrygdRammeService,
                    saksnummerService = benyttetSaksnummerService,
                    overføringRepository = benyttetOverføringRepository
                ),
                saksnummerRepository = benyttetSaksnummerRepository,
                saksnummerService = benyttetSaksnummerService,
                tilgangsstyringRestClient = benyttetTilgangsstyringRestClient,
                koronaoverføringRepository = benyttetKoronaoverføringRepository,
                spleisetKoronaOverføringerService = spleisetKoronaOverføringerService
                    ?: SpleisetKoronaOverføringerService(
                        koronaoverføringRepository = benyttetKoronaoverføringRepository,
                        saksnummerService = benyttetSaksnummerService,
                        infotrygdRammeService = benyttetInfotrygdRammeService
                    )
            )
        }
    }
}