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
import io.ktor.response.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.omsorgspenger.aleneom.apis.SpleisetAleneOmOmsorgenApi
import no.nav.omsorgspenger.midlertidigalene.rivers.InitierMidlertidigAlene
import no.nav.omsorgspenger.overføringer.apis.OverføringApi

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
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        statistikkService = applicationContext.statistikkService
    )
    InitierMidlertidigAlene(
        rapidsConnection = this,
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
        }

        authenticate(*accessAsPersonIssuers.allIssuers()) {
            OverføringApi(
                overføringRepository = applicationContext.overføringRepository,
                saksnummerService = applicationContext.saksnummerService
            )
        }
    }
}