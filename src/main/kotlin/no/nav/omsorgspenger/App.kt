package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.KafkaBuilder.kafkaProducer
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.GjennomførOverføringService
import no.nav.omsorgspenger.overføringer.OverføringerApi
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.InitierOverføringAvOmsorgsdager
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.URI
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.helse.rapids_rivers.KtorBuilder
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenApi
import no.nav.omsorgspenger.overføringer.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import javax.sql.DataSource
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenService
import no.nav.omsorgspenger.overføringer.OverføringService
import no.nav.omsorgspenger.saksnummer.SaksnummerService

fun main() = when (System.getenv("RAPIDS_DISABLED") == "true") {
    true -> ktorApplication()
    else -> rapidsApplication()
}

private fun ktorApplication() {
    embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                CollectorRegistry.defaultRegistry,
                Clock.SYSTEM
            )
            meterBinders = listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics()
            )
        }
        routing {
            get ("/isalive") {
                call.respondText("ALIVE")
            }
            get ("/isready") {
                call.respondText("READY")
            }
            get("/metrics") {
                val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
                call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                    TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
                }
            }
        }
    }.start(wait = true)
}

private fun rapidsApplication() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule { omsorgspengerRammemeldinger(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    InitierOverføringAvOmsorgsdager(
        rapidsConnection = this,
        fordelingService = applicationContext.fordelingService,
        utvidetRettService = applicationContext.utvidetRettService,
        midlertidigAleneService = applicationContext.midlertidigAleneService
    )
    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        gjennomførOverføringService = applicationContext.gjennomførOverføringService,
        saksnummerRepository = applicationContext.saksnummerRepository
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this,
        formidlingService = applicationContext.formidlingService
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

internal fun Application.omsorgspengerRammemeldinger(applicationContext: ApplicationContext) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        }
    }
    routing {
        HealthRoute(healthService = applicationContext.healthService)
        OverføringerApi(overføringService = applicationContext.overføringService) // todo: autentisering
        AleneOmOmsorgenApi(aleneOmOmsorgenService = applicationContext.aleneOmOmsorgenService) // todo: autentisering
    }
}

internal class ApplicationContext(
    internal val env: Environment,
    internal val accessTokenClient: AccessTokenClient,
    internal val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway,
    internal val infotrygdRammeService: InfotrygdRammeService,
    internal val fordelingService: FordelingService,
    internal val utvidetRettService: UtvidetRettService,
    internal val midlertidigAleneService: MidlertidigAleneService,
    internal val gjennomførOverføringService: GjennomførOverføringService,
    internal val overføringRepository: OverføringRepository,
    internal val overføringService: OverføringService,
    internal val aleneOmOmsorgenService: AleneOmOmsorgenService,
    internal val kafkaProducer: KafkaProducer<String, String>,
    internal val formidlingService: FormidlingService,
    internal val saksnummerRepository: SaksnummerRepository,
    internal val saksnummerService: SaksnummerService,
    internal val dataSource: DataSource,
    internal val healthService: HealthService) {

    internal fun start() {
        dataSource.migrate()
    }
    internal fun stop() {
        kafkaProducer.close()
    }

    internal class Builder(
        internal var env: Environment? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway? = null,
        internal var infotrygdRammeService: InfotrygdRammeService? = null,
        internal var fordelingService: FordelingService? = null,
        internal var utvidetRettService: UtvidetRettService? = null,
        internal var midlertidigAleneService: MidlertidigAleneService? = null,
        internal var gjennomførOverføringService: GjennomførOverføringService? = null,
        internal var overføringRepository: OverføringRepository? = null,
        internal var overføringService: OverføringService? = null,
        internal var aleneOmOmsorgenService: AleneOmOmsorgenService? = null,
        internal var kafkaProducer: KafkaProducer<String, String>? = null,
        internal var formidlingService: FormidlingService? = null,
        internal var saksnummerRepository: SaksnummerRepository? = null,
        internal var saksnummerService: SaksnummerService? = null,
        internal var dataSource: DataSource? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetAccessTokenClient = accessTokenClient?:ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
            )
            val benyttetOmsorgspengerInfotrygdRammevedtakGateway = omsorgspengerInfotrygdRammevedtakGateway?:OmsorgspengerInfotrygdRammevedtakGateway(
                accessTokenClient = benyttetAccessTokenClient,
                hentRammevedtakFraInfotrygdScopes = benyttetEnv.hentRequiredEnv("HENT_RAMMEVEDTAK_FRA_INFOTRYGD_SCOPES").csvTilSet(),
                omsorgspengerInfotrygdRammevedtakBaseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_INFOTRYGD_RAMMEVEDTAK_BASE_URL"))
            )
            val benyttetInfotrygdRammeService = infotrygdRammeService?:InfotrygdRammeService(
                omsorgspengerInfotrygdRammevedtakGateway = benyttetOmsorgspengerInfotrygdRammevedtakGateway
            )

            val benyttetKafkaProducer =  kafkaProducer ?: benyttetEnv.kafkaProducer()

            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()

            val benyttetOverføringRepository = overføringRepository ?: OverføringRepository(
                dataSource = benyttetDataSource
            )
            val benyttetAleneOmOmsorgenService = aleneOmOmsorgenService ?: AleneOmOmsorgenService(benyttetInfotrygdRammeService)

            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(
                dataSource = benyttetDataSource
            )

            val benyttetSaksnummerService = saksnummerService ?: SaksnummerService(
                saksnummerRepository = benyttetSaksnummerRepository
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
                aleneOmOmsorgenService = benyttetAleneOmOmsorgenService,
                healthService = HealthService(healthChecks = setOf(
                    benyttetOmsorgspengerInfotrygdRammevedtakGateway
                )),
                kafkaProducer = benyttetKafkaProducer,
                formidlingService = formidlingService ?: FormidlingService(
                    kafkaProducer = benyttetKafkaProducer
                ),
                dataSource = benyttetDataSource,
                overføringRepository = benyttetOverføringRepository,
                overføringService = overføringService ?: OverføringService(
                    infotrygdRammeService = benyttetInfotrygdRammeService,
                    saksnummerService = benyttetSaksnummerService,
                    overføringRepository = benyttetOverføringRepository
                ),
                saksnummerRepository = benyttetSaksnummerRepository,
                saksnummerService = benyttetSaksnummerService
            )
        }
    }
}