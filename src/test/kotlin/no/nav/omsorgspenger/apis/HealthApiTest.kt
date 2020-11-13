package no.nav.omsorgspenger.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.server.testing.*
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.WireMockExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class HealthApiTest(
    private val dataSource: DataSource,
    private val wireMockServer: WireMockServer) {

    @Test
    fun `Test health end point`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestApplicationContextBuilder(
                dataSource = dataSource.cleanAndMigrate(),
                wireMockServer = wireMockServer
            ).build())
        }) {
            handleRequest(Get, "/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            }
        }
    }
}