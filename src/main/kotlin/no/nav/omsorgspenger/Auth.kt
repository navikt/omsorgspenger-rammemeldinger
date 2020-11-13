package no.nav.omsorgspenger

import no.nav.helse.dusseldorf.ktor.auth.Azure
import no.nav.k9.rapid.river.hentRequiredEnv
import java.net.URI

internal object Issuers {
    internal fun azureV2K9Aarskvantm(env: Map<String, String>) = Azure(
        issuer = env.hentRequiredEnv("AZURE_V2_ISSUER"),
        jwksUri = URI(env.hentRequiredEnv("AZURE_V2_JWKS_URI")),
        audience = env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = "azure-v2-k9-aarskvantum",
        authorizedClients = setOf(env.hentRequiredEnv("AZURE_K9_AARSKVANTUM_CLIENT_ID")),
        requiredScopes = setOf(),
        requireCertificateClientAuthentication = false,
        requiredGroups = setOf(),
        requiredRoles = setOf()
    )
}