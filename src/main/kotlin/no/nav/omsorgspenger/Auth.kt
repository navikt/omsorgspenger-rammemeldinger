package no.nav.omsorgspenger

import no.nav.helse.dusseldorf.ktor.auth.Azure
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import java.net.URI

internal object Issuers {

    internal fun accessAsApplication(env: Map<String, String>) =
        mapOf("access_as_application".let { alias ->
            alias to AzureV2(
                alias = alias,
                env = env,
                requiredRoles = setOf(
                    "access_as_application"
                )
            )
    }).withoutAdditionalClaimRules()

    internal fun accessAsPerson(env: Map<String, String>) =
        mapOf("access_as_person".let { alias ->
            alias to AzureV2(
                alias = alias,
                env = env,
                requiredRoles = setOf()
            )
    }).withoutAdditionalClaimRules()

    private fun AzureV2(
        alias: String,
        requiredRoles: Set<String>,
        env: Map<String, String>) = Azure(
        issuer = env.azureV2Issuer(),
        jwksUri = env.azureV2JwksUri(),
        audience = env.azureV2Audience(),
        alias = alias,
        authorizedClients = setOf(),
        requiredScopes = setOf(),
        requireCertificateClientAuthentication = false,
        requiredGroups = setOf(),
        requiredRoles = requiredRoles
    )
    private fun Environment.azureV2Issuer() = hentRequiredEnv("AZURE_OPENID_CONFIG_ISSUER")
    private fun Environment.azureV2JwksUri() = URI(hentRequiredEnv("AZURE_OPENID_CONFIG_JWKS_URI"))
    private fun Environment.azureV2Audience() = hentRequiredEnv("AZURE_APP_CLIENT_ID")
}