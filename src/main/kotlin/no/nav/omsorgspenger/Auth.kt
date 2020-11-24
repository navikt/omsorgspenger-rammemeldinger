package no.nav.omsorgspenger

import no.nav.helse.dusseldorf.ktor.auth.Azure
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import java.net.URI

internal object Issuers {
    private fun Environment.azureV2Issuer() = hentRequiredEnv("AZURE_V2_ISSUER")
    private fun Environment.azureV2JwksUri() = URI(hentRequiredEnv("AZURE_V2_JWKS_URI"))
    private fun Environment.azureV2Audience() = hentRequiredEnv("AZURE_APP_CLIENT_ID")

    internal fun accessAsApplication(env: Map<String, String>) =
        mapOf("access_as_application".let { alias ->
            alias to Azure(
                issuer = env.azureV2Issuer(),
                jwksUri = env.azureV2JwksUri(),
                audience = env.azureV2Audience(),
                alias = alias,
                authorizedClients = setOf(),
                requiredScopes = setOf(),
                requireCertificateClientAuthentication = false,
                requiredGroups = setOf(),
                requiredRoles = setOf(
                    "access_as_application"
                )
            )

    }).withoutAdditionalClaimRules()
}