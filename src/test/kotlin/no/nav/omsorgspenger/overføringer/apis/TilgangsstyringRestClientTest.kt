package no.nav.omsorgspenger.overføringer.apis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.testutils.AuthorizationHeaders.authorizedUser
import no.nav.omsorgspenger.testutils.WireMockExtension
import no.nav.omsorgspenger.testutils.personident403
import no.nav.omsorgspenger.testutils.tilgangApiBaseUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(WireMockExtension::class)
internal class TilgangsstyringRestClientTest(
        private val wireMockServer: WireMockServer
) {

    private val tilgangsstyringRestClient = TilgangsstyringRestClient(
            env = mapOf("TILGANGSSTYRING_URL" to wireMockServer.tilgangApiBaseUrl()),
            httpClient = HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(jacksonObjectMapper()) }
            }
    )

    @Test
    fun `Boolean false for de som ikke som skall ha tillgang`() {
        val harTilgang = runBlocking {
            tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf(personident403),
                authHeader = authorizedUser(),
                beskrivelse = "test")
        }

        assertFalse(harTilgang)
    }

    @Test
    fun `Boolean true for de som skall ha tilgang`() {
        val harTilgang = runBlocking {
            tilgangsstyringRestClient.sjekkTilgang(
                    identer = setOf("123123"),
                    authHeader = authorizedUser(),
                    beskrivelse = "test")
        }

        assert(harTilgang)
    }
}