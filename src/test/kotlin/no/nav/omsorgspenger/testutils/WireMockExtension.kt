package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class WireMockExtension : ParameterResolver {

    internal companion object {

        private val wireMockServer = WireMockBuilder().withAzureSupport().build()

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    wireMockServer.stop()
                }
            )
        }

        private val støttedeParametre = listOf(
            WireMockServer::class.java
        )
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return wireMockServer
    }
}