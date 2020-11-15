package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.overføringer.OverføringerApi
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.InitierOverføringAvOmsorgsdager
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenApi

fun main() = when (System.getenv("RAPIDS_APPLICATION") == "disabled") {
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
        midlertidigAleneService = applicationContext.midlertidigAleneService,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        gjennomførOverføringService = applicationContext.gjennomførOverføringService,
        saksnummerRepository = applicationContext.saksnummerRepository,
        aleneOmOmsorgenRepository = applicationContext.aleneOmOmsorgenRepository,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this,
        formidlingService = applicationContext.formidlingService,
        behovssekvensRepository = applicationContext.behovssekvensRepository
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

    install(StatusPages) {
        exception<ClaimEnforcementFailed> { cause ->
            log.error("Request uten tilstrekkelig tilganger stoppet. Håndheving av regler resulterte i '${cause.outcomes}'")
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    val azureV2K9Aarskvantum = Issuers.azureV2K9Aarskvantm(
        env = applicationContext.env
    )

    val accessAsApplicationIssuers = mapOf(
        azureV2K9Aarskvantum.alias() to azureV2K9Aarskvantum
    ).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(accessAsApplicationIssuers)
    }

    routing {
        HealthRoute(healthService = applicationContext.healthService)
        authenticate(*accessAsApplicationIssuers.allIssuers()) {
            OverføringerApi(overføringService = applicationContext.overføringService)
            AleneOmOmsorgenApi(aleneOmOmsorgenService = applicationContext.aleneOmOmsorgenService)
        }
    }
}