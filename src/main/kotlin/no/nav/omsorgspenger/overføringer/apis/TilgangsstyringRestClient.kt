package no.nav.omsorgspenger.overføringer.apis

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentOptionalEnv
import no.nav.k9.rapid.river.hentRequiredEnv

internal class TilgangsstyringRestClient(
        private val httpClient: HttpClient,
        env: Environment
): HealthCheck {

    private val logger = LoggerFactory.getLogger(TilgangsstyringRestClient::class.java)
    private val tilgangUrl = env.hentOptionalEnv("TILGANGSSTYRING_URL")

    internal suspend fun sjekkTilgang(identer: Set<String>, authHeader: String, beskrivelse: String): Boolean {
        return kotlin.runCatching {
            httpClient.post<HttpStatement>("$tilgangUrl/api/tilgang/personer") {
                header(HttpHeaders.Authorization, authHeader)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                body = PersonerRequestBody(identer, Operasjon.Visning, beskrivelse)
            }.execute()
        }.håndterResponse()
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): Boolean = fold(
            onSuccess = { response ->
                when (response.status) {
                    HttpStatusCode.NoContent -> true
                    HttpStatusCode.Forbidden -> false
                    else -> {
                        response.logError()
                        throw RuntimeException("Uventet response code (${response.status}) ved tilgangssjekk")
                    }
                }
            },
            onFailure = { cause ->
                when (cause is ResponseException) {
                    true -> {
                        cause.response.logError()
                        throw RuntimeException("Uventet feil ved tilgangssjekk")
                    }
                    else -> throw cause
                }
            }
    )

    private suspend fun HttpResponse.logError() =
            logger.error("HTTP ${status.value} fra omsorgspenger-tilgangsstyring, response: ${String(content.toByteArray())}")

    override suspend fun check(): no.nav.helse.dusseldorf.ktor.health.Result {
        return kotlin.runCatching {
            httpClient.get<HttpStatement>("$tilgangUrl/isalive").execute()
        }.fold(
                onSuccess = { response ->
                    when (HttpStatusCode.OK == response.status) {
                        true -> Healthy("TilgangsstyringRestClient", "OK")
                        false -> UnHealthy("TilgangsstyringRestClient", "Feil: Mottok Http Status Code ${response.status.value}")
                    }
                },
                onFailure = {
                    UnHealthy("TilgangsstyringRestClient", "Feil: ${it.message}")
                }
        )
    }
}

enum class Operasjon {
    Visning
}

data class PersonerRequestBody(
        val identitetsnummer: Set<String>,
        val operasjon: Operasjon,
        val beskrivelse: String
)