package no.nav.omsorgspenger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.KafkaBuilder.kafkaProducerOnPrem
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenRepository
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenService
import no.nav.omsorgspenger.aleneom.apis.SpleisetAleneOmOmsorgenService
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.formidling.KafkaFormidlingService
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerService
import no.nav.omsorgspenger.koronaoverføringer.db.KoronaoverføringRepository
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.GjennomførOverføringService
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.statistikk.StatistikkService
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.URI
import javax.sql.DataSource
import no.nav.omsorgspenger.overføringer.apis.TilgangsstyringRestClient
import no.nav.omsorgspenger.statistikk.KafkaStatistikkService

internal class ApplicationContext(
    internal val env: Environment,
    internal val accessTokenClient: AccessTokenClient,
    internal val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway,
    internal val infotrygdRammeService: InfotrygdRammeService,
    internal val fordelingService: FordelingService,
    internal val utvidetRettService: UtvidetRettService,
    internal val midlertidigAleneService: MidlertidigAleneService,
    internal val behovssekvensRepository: BehovssekvensRepository,
    internal val gjennomførOverføringService: GjennomførOverføringService,
    internal val overføringRepository: OverføringRepository,
    internal val spleisetOverføringerService: SpleisetOverføringerService,
    internal val statistikkService: StatistikkService,
    internal val aleneOmOmsorgenRepository: AleneOmOmsorgenRepository,
    internal val aleneOmOmsorgenService: AleneOmOmsorgenService,
    internal val spleisetAleneOmOmsorgenService: SpleisetAleneOmOmsorgenService,
    internal val kafkaProducerOnPrem: KafkaProducer<String, String>,
    internal val formidlingService: FormidlingService,
    internal val saksnummerRepository: SaksnummerRepository,
    internal val saksnummerService: SaksnummerService,
    internal val tilgangsstyringRestClient: TilgangsstyringRestClient,
    internal val dataSource: DataSource,
    internal val healthService: HealthService,
    internal val koronaoverføringRepository: KoronaoverføringRepository,
    internal val spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService) {

    internal fun start() {
        dataSource.migrate()
    }
    internal fun stop() {
        kafkaProducerOnPrem.close()
    }

    internal class Builder(
        internal var env: Environment? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
        internal var infotrygdRammeService: InfotrygdRammeService? = null,
        internal var fordelingService: FordelingService? = null,
        internal var utvidetRettService: UtvidetRettService? = null,
        internal var midlertidigAleneService: MidlertidigAleneService? = null,
        internal var behovssekvensRepository: BehovssekvensRepository? = null,
        internal var gjennomførOverføringService: GjennomførOverføringService? = null,
        internal var overføringRepository: OverføringRepository? = null,
        internal var spleisetOverføringerService: SpleisetOverføringerService? = null,
        internal var tilgangsstyringRestClient: TilgangsstyringRestClient? = null,
        internal var statistikkService: StatistikkService? = null,
        internal var aleneOmOmsorgenRepository: AleneOmOmsorgenRepository? = null,
        internal var aleneOmOmsorgenService: AleneOmOmsorgenService? = null,
        internal var spleisetAleneOmOmsorgenService: SpleisetAleneOmOmsorgenService? = null,
        internal var kafkaProducerOnPrem: KafkaProducer<String, String>? = null,
        internal var formidlingService: FormidlingService? = null,
        internal var saksnummerRepository: SaksnummerRepository? = null,
        internal var saksnummerService: SaksnummerService? = null,
        internal var dataSource: DataSource? = null,
        internal var koronaoverføringRepository: KoronaoverføringRepository? = null,
        internal var spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))
            )
            val benyttetOmsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway?: OmsorgspengerInfotrygdRammevedtakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES").csvTilSet(),
                omsorgspengerInfotrygdRammevedtakBaseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL"))
            )
            val benyttetInfotrygdRammeService = infotrygdRammeService?: InfotrygdRammeService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )

            val benyttetKafkaProducerOnPrem = kafkaProducerOnPrem ?: benyttetEnv.kafkaProducerOnPrem("omsorgspenger-rammemeldinger-onprem")

            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()

            val benyttetOverføringRepository = overføringRepository ?: OverføringRepository(
                dataSource = benyttetDataSource
            )

            val benyttetStatistikkService = statistikkService ?: KafkaStatistikkService(
                kafkaProducer = benyttetKafkaProducerOnPrem
            )

            val benyttetAleneOmOmsorgenRepository = aleneOmOmsorgenRepository ?: AleneOmOmsorgenRepository(
                dataSource = benyttetDataSource
            )

            val benyttetAleneOmOmsorgenService = aleneOmOmsorgenService ?: AleneOmOmsorgenService(
                aleneOmOmsorgenRepository = benyttetAleneOmOmsorgenRepository
            )

            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(
                dataSource = benyttetDataSource
            )

            val benyttetSaksnummerService = saksnummerService ?: SaksnummerService(
                saksnummerRepository = benyttetSaksnummerRepository
            )

            val benyttetTilgangsstyringRestClient = tilgangsstyringRestClient ?: TilgangsstyringRestClient(
                httpClient = HttpClient {
                    install(JsonFeature) { serializer = JacksonSerializer(jacksonObjectMapper()) }
                },
                env = benyttetEnv
            )

            val benyttetKoronaoverføringRepository = koronaoverføringRepository ?: KoronaoverføringRepository(
                dataSource = benyttetDataSource
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
                gjennomførOverføringService = gjennomførOverføringService ?: GjennomførOverføringService(
                    overføringRepository = benyttetOverføringRepository
                ),
                aleneOmOmsorgenRepository = benyttetAleneOmOmsorgenRepository,
                aleneOmOmsorgenService = benyttetAleneOmOmsorgenService,
                spleisetAleneOmOmsorgenService = spleisetAleneOmOmsorgenService ?: SpleisetAleneOmOmsorgenService(
                    infotrygdRammeService = benyttetInfotrygdRammeService,
                    aleneOmOmsorgenRepository = benyttetAleneOmOmsorgenRepository,
                    saksnummerService = benyttetSaksnummerService
                ),
                healthService = HealthService(healthChecks = setOf(
                    benyttetOmsorgspengerInfotrygdRammevedtakGateway
                )),
                kafkaProducerOnPrem = benyttetKafkaProducerOnPrem,
                formidlingService = formidlingService ?: KafkaFormidlingService(
                    kafkaProducer = benyttetKafkaProducerOnPrem
                ),
                dataSource = benyttetDataSource,
                overføringRepository = benyttetOverføringRepository,
                spleisetOverføringerService = spleisetOverføringerService ?: SpleisetOverføringerService(
                    infotrygdRammeService = benyttetInfotrygdRammeService,
                    saksnummerService = benyttetSaksnummerService,
                    overføringRepository = benyttetOverføringRepository
                ),
                statistikkService = benyttetStatistikkService,
                saksnummerRepository = benyttetSaksnummerRepository,
                saksnummerService = benyttetSaksnummerService,
                behovssekvensRepository = behovssekvensRepository ?: BehovssekvensRepository(
                    dataSource = benyttetDataSource
                ),
                tilgangsstyringRestClient = benyttetTilgangsstyringRestClient,
                koronaoverføringRepository = benyttetKoronaoverføringRepository,
                spleisetKoronaOverføringerService = spleisetKoronaOverføringerService ?: SpleisetKoronaOverføringerService(
                    koronaoverføringRepository = benyttetKoronaoverføringRepository,
                    saksnummerService = benyttetSaksnummerService,
                    infotrygdRammeService = benyttetInfotrygdRammeService
                )
            )
        }
    }
}