package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.jws.Azure

internal object AuthorizationHeaders {
    internal fun k9AarskvantumAuthorized() = Azure.V2_0.generateJwt(
        clientId = "k9-aarskvantum",
        audience = "omsorgspenger-rammemeldinger",
        clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
        accessAsApplication = true
    ).let { "Bearer $it" }

    internal fun k9AarskvantumUnauthorized() = Azure.V2_0.generateJwt(
        clientId = "k9-aarskvantum",
        audience = "omsorgspenger-infotrygd-rammemeldinger",
        clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
        accessAsApplication = false
    ).let { "Bearer $it" }
}