package no.nav.omsorgspenger.testutils

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.DataSourceBuilder
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

internal class ApplicationContextExtension : ParameterResolver {

    internal companion object {

        private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(tempDir)
            .setDataDirectory(tempDir.resolve("datadir"))
            .start()

        private val embeddedPostgres = embeddedPostgress(createTempDir("tmp_postgres"))

        private val applicationContextBuilder = TestApplicationContextBuilder().also { builder ->
            builder.dataSource = DataSourceBuilder(mapOf(
                "DATABASE_HOST" to "localhost",
                "DATABASE_PORT" to "${embeddedPostgres.port}",
                "DATABASE_DATABASE" to "postgres",
                "DATABASE_USERNAME" to "postgres",
                "DATABASE_PASSWORD" to "postgres"
            )).build()
        }

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    embeddedPostgres.postgresDatabase.connection.close()
                    embeddedPostgres.close()
                }
            )
        }

        private val støttedeParametre = listOf(
            ApplicationContext.Builder::class.java
        )
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return applicationContextBuilder
    }
}