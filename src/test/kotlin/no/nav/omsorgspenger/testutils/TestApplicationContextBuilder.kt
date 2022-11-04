package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenResponse
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal fun TestApplicationContextBuilder(
    dataSource: DataSource,
    wireMockServer: WireMockServer? = null,
    additionalEnv: Map<String, String> = emptyMap()
) = ApplicationContext.Builder(
    formidlingService = RecordingFormidlingService(),
    statistikkService = RecordingStatistikkService(),
    accessTokenClient = mockk<AccessTokenClient>().also {
        every { it.getAccessToken(any()) }.returns(
            AccessTokenResponse(
                accessToken = "foo",
                expiresIn = 1000,
                tokenType = "Bearer"
            )
        )
    },
    kafkaProducerStatistikk = mockk(),
    omsorgspengerInfotrygdRammevedtakGateway = mockk<OmsorgspengerInfotrygdRammevedtakGateway>().also {
        coEvery { it.hent(any(), any(), any()) }.returns(listOf())
        coEvery { it.check() }.returns(Healthy("OmsorgspengerInfotrygdRammevedtakGateway", "Mock helsesjekk OK!"))
    },
    dataSource = dataSource,
    env = when (wireMockServer) {
        null -> mapOf(
            "TILGANGSSTYRING_URL" to "test"
        )

        else -> mapOf(
            "AZURE_OPENID_CONFIG_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_OPENID_CONFIG_JWKS_URI" to wireMockServer.getAzureV2JwksUrl(),
            "AZURE_APP_CLIENT_ID" to "omsorgspenger-rammemeldinger",
            "TILGANGSSTYRING_URL" to wireMockServer.tilgangApiBaseUrl()
        )
    }.plus(additionalEnv).plus(
        mapOf(
            "TILGANGSSTYRING_SCOPES" to "tilgangsstyring./default",
            "OVERFORING_BEHANDLE_MOTTATT_ETTER" to "2000-01-01",
            "KORONA_BEHANDLE_MOTTATT_ETTER" to "2000-01-01"
        )
    )
)

internal fun DataSource.cleanAndMigrate() = this.also {
    Flyway
        .configure()
        .cleanDisabled(false)
        .dataSource(this)
        .load()
        .also {
            it.clean()
            it.migrate()
        }
}