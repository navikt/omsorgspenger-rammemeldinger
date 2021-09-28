package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath

private const val apiPath = "/api/tilgang/personer"

internal const val personident403 = "40340340340"

internal const val highPriority = 1
internal const val catchAllPriority = 9

private fun WireMockServer.stubTilgangsstyringOk(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$apiPath.*")
        ).atPriority(catchAllPriority)
            .willReturn(
                aResponse()
                    .withStatus(204)
                    .withHeader("Content-Type", "application/json")
            )
    )

    return this
}

private fun WireMockServer.stubTilgangsstyringIkkeTilgang(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$apiPath.*")
        ).atPriority(highPriority)
            .withRequestBody(matchingJsonPath("$.identitetsnummer", containing("$personident403")))
            .willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
            )
    )

    return this
}

internal fun WireMockServer.stubTilgangApi() = stubTilgangsstyringOk()
    .stubTilgangsstyringIkkeTilgang()

internal fun WireMockServer.tilgangApiBaseUrl() = baseUrl() + apiPath
