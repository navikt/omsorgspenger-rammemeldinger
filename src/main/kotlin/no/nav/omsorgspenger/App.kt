package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.server.application.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerApi
import no.nav.omsorgspenger.overføringer.rivers.PubliserOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.BehandleOverføringAvOmsorgsdager
import no.nav.omsorgspenger.overføringer.rivers.InitierOverføringAvOmsorgsdager
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.response.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.k9.rapid.river.hentOptionalEnv
import no.nav.omsorgspenger.aleneom.apis.AleneOmOmsorgenApi
import no.nav.omsorgspenger.fordelinger.rivers.InitierFordelingAvOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.apis.KoronaOverføringerApi
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerApi
import no.nav.omsorgspenger.koronaoverføringer.rivers.BehandleOpphøreKoronaOverføringer
import no.nav.omsorgspenger.koronaoverføringer.rivers.BehandleOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.rivers.InitierOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.koronaoverføringer.rivers.PubliserOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.overføringer.apis.OverføringerApi
import no.nav.omsorgspenger.overføringer.rivers.BehandleOpphøreOverføringer
import java.time.LocalDate

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.create(
        env = applicationContext.env,
        builder = { withKtorModule { omsorgspengerRammemeldinger(applicationContext) } }
    )
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
        behandleMottattEtter = LocalDate.parse(
            applicationContext.env.hentOptionalEnv("OVERFORING_BEHANDLE_MOTTATT_ETTER") ?: "2021-01-24"
        )
    )

    BehandleOverføringAvOmsorgsdager(
        rapidsConnection = this,
        gjennomførOverføringService = applicationContext.gjennomførOverføringService,
        saksnummerRepository = applicationContext.saksnummerRepository,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    PubliserOverføringAvOmsorgsdager(
        rapidsConnection = this,
        formidlingService = applicationContext.formidlingService,
        behovssekvensRepository = applicationContext.behovssekvensRepository
    )
    BehandleOpphøreOverføringer(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        overføringerRepository = applicationContext.overføringRepository
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
    InitierOverføreKoronaOmsorgsdager(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        utvidetRettService = applicationContext.utvidetRettService,
        fordelingService = applicationContext.fordelingService,
        spleisetOverføringerService = applicationContext.spleisetOverføringerService,
        spleisetKoronaOverføringerService = applicationContext.spleisetKoronaOverføringerService,
        behandleMottattEtter = LocalDate.parse(
            applicationContext.env.hentOptionalEnv("KORONA_BEHANDLE_MOTTATT_ETTER") ?: "2021-01-25"
        )
    )
    BehandleOverføreKoronaOmsorgsdager(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        koronaoverføringRepository = applicationContext.koronaoverføringRepository,
        saksnummerRepository = applicationContext.saksnummerRepository
    )
    PubliserOverføreKoronaOmsorgsdager(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        formidlingService = applicationContext.formidlingService
    )
    BehandleOpphøreKoronaOverføringer(
        rapidsConnection = this,
        behovssekvensRepository = applicationContext.behovssekvensRepository,
        koronaoverføringRepository = applicationContext.koronaoverføringRepository
    )
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
        exception<ClaimEnforcementFailed> { call, cause ->
            call.application.environment.log.error("Request uten tilstrekkelig tilganger stoppet. Håndheving av regler resulterte i '${cause.outcomes}'")
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    install(CallId) {
        header("callId")
    }

    install(CallLogging) {
        logRequests()
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

    preStopOnApplicationStopPreparing(
        preStopActions = listOf(
            FullførAktiveRequester(this)
        )
    )

    routing {
        HealthRoute(healthService = applicationContext.healthService)

        authenticate(*accessAsApplicationIssuers.allIssuers()) {
            SpleisetOverføringerApi(
                spleisetOverføringerService = applicationContext.spleisetOverføringerService
            )
            AleneOmOmsorgenApi(
                aleneOmOmsorgenRepository = applicationContext.aleneOmOmsorgenRepository
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