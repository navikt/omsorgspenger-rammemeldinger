package no.nav.omsorgspenger.overføringer.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.omsorgspenger.testutils.AuthorizationHeaders.authorizedUser
import no.nav.omsorgspenger.testutils.WireMockExtension
import no.nav.omsorgspenger.testutils.personident403
import no.nav.omsorgspenger.testutils.tilgangApiBaseUrl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI

@ExtendWith(WireMockExtension::class)
internal class TilgangsstyringRestClientTest(
    wireMockServer: WireMockServer
) {

    private val tilgangsstyringRestClient = TilgangsstyringRestClient(
        env = mapOf(
            "TILGANGSSTYRING_URL" to wireMockServer.tilgangApiBaseUrl(),
            "TILGANGSSTYRING_SCOPES" to "tilgangsstyring/.default"
        ),
        httpClient = HttpClient {
            install(ContentNegotiation) {
                jackson()
            }
        },
        accessTokenClient = ClientSecretAccessTokenClient(
            clientId = "foo",
            clientSecret = "bar",
            tokenEndpoint = URI(wireMockServer.getAzureV2TokenUrl()),
            authenticationMode = ClientSecretAccessTokenClient.AuthenticationMode.POST
        )
    )

    @Test
    fun `Boolean false for de som ikke som skall ha tillgang`() {
        val harTilgang = runBlocking {
            tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf(personident403),
                authHeader = authorizedUser(),
                beskrivelse = "test",
                correlationId = "foo-bar"
            )
        }

        assertFalse(harTilgang)
    }

    @Test
    fun `Boolean true for de som skall ha tilgang`() {
        val harTilgang = runBlocking {
            tilgangsstyringRestClient.sjekkTilgang(
                identer = setOf("123123"),
                authHeader = authorizedUser(),
                beskrivelse = "test",
                correlationId = "foo-bar"
            )
        }

        assertTrue(harTilgang)
    }
}