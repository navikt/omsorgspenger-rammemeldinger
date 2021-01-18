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
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerApi
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.InitierOverføringAvOmsorgsdager
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.k9.rapid.river.hentOptionalEnv
import no.nav.omsorgspenger.aleneom.apis.SpleisetAleneOmOmsorgenApi
import no.nav.omsorgspenger.fordelinger.rivers.InitierFordelingAvOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.apis.KoronaOverføringerApi
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerApi
import no.nav.omsorgspenger.koronaoverføringer.rivers.BehandleOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.rivers.InitierOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.rivers.PubliserOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.midlertidigalene.rivers.InitierMidlertidigAlene
import no.nav.omsorgspenger.overføringer.apis.OverføringerApi
import org.slf4j.event.Level
import java.util.*

fun main() {
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
        spleisetKoronaOverføringerService = applicationContext.spleisetKoronaOverføringerService,
        utvidetRettService = applicationContext.utvidetRettService,
        midlertidigAleneService = applicationContext.midlertidigAleneService,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        enableBehandling = applicationContext.env.hentOptionalEnv("OVERFORING_BEHANDLING") == "enabled"
    )

    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        gjennomførOverføringService = applicationContext.gjennomførOverføringService,
        saksnummerRepository = applicationContext.saksnummerRepository,
        aleneOmOmsorgenService = applicationContext.aleneOmOmsorgenService,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this,
        formidlingService = applicationContext.formidlingService,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        statistikkService = applicationContext.statistikkService
    )
    InitierMidlertidigAlene(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    InitierFordelingAvOmsorgsdager(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    registerOverføreKoronaOmsorgsdager(
        applicationContext = applicationContext
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

internal fun RapidsConnection.registerOverføreKoronaOmsorgsdager(applicationContext: ApplicationContext) {
    val enableBehandling = applicationContext.env.hentOptionalEnv("KORONA_BEHANDLING") == "enabled"
    InitierOverføreKoronaOmsorgsdager(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        utvidetRettService = applicationContext.utvidetRettService,
        fordelingService = applicationContext.fordelingService,
        spleisetOverføringerService = applicationContext.spleisetOverføringerService,
        spleisetKoronaOverføringerService = applicationContext.spleisetKoronaOverføringerService,
        enableBehandling = enableBehandling
    )
    if (enableBehandling) {
        BehandleOverføreKoronaOmsorgsdager(
            rapidsConnection = this,
            behovssekvensRepository = applicationContext.behovssekvensRepository,
            koronaoverføringRepository = applicationContext.koronaoverføringRepository,
            saksnummerRepository = applicationContext.saksnummerRepository,
            aleneOmOmsorgenService = applicationContext.aleneOmOmsorgenService
        )
        PubliserOverføreKoronaOmsorgsdager(
            rapidsConnection = this,
            behovssekvensRepository = applicationContext.behovssekvensRepository,
            formidlingService = applicationContext.formidlingService
        )
    }
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

    install(CallId) {
        retrieve { when {
            it.request.headers.contains(HttpHeaders.XCorrelationId) -> it.request.header(HttpHeaders.XCorrelationId)
            it.request.headers.contains("Nav-Call-Id") -> it.request.header("Nav-Call-Id")
            else -> "rammemeldinger-${UUID.randomUUID()}"
        }}
    }

    install(CallLogging) {
        val ignorePaths = setOf("/isalive", "/isready", "/metrics")
        level = Level.INFO
        logger = log
        filter { call -> !ignorePaths.contains(call.request.path().toLowerCase()) }
        callIdMdc("correlation_id")
        callIdMdc("callId")
    }

    val accessAsApplicationIssuers = Issuers.accessAsApplication(
        env = applicationContext.env
    )

    val accessAsPersonIssuers = Issuers.accessAsPerson(
        env = applicationContext.env
    )

    install(Authentication) {
        multipleJwtIssuers(accessAsApplicationIssuers.plus(accessAsPersonIssuers))
    }

    routing {
        HealthRoute(healthService = applicationContext.healthService)

        authenticate(*accessAsApplicationIssuers.allIssuers()) {
            SpleisetOverføringerApi(
                spleisetOverføringerService = applicationContext.spleisetOverføringerService
            )
            SpleisetAleneOmOmsorgenApi(
                spleisetAleneOmOmsorgenService = applicationContext.spleisetAleneOmOmsorgenService
            )
            SpleisetKoronaOverføringerApi(
                spleisetKoronaOverføringerService = applicationContext.spleisetKoronaOverføringerService
            )
        }

        authenticate(*accessAsPersonIssuers.allIssuers()) {
            OverføringerApi(
                overføringRepository = applicationContext.overføringRepository,
                saksnummerService = applicationContext.saksnummerService,
                tilgangsstyringRestClient = applicationContext.tilgangsstyringRestClient
            )
            KoronaOverføringerApi(
                koronaoverføringRepository = applicationContext.koronaoverføringRepository,
                saksnummerService = applicationContext.saksnummerService,
                tilgangsstyringRestClient = applicationContext.tilgangsstyringRestClient
            )
        }
    }
}