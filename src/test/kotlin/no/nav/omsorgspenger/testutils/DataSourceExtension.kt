package no.nav.omsorgspenger.testutils

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.omsorgspenger.DataSourceBuilder
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.nio.file.Files.createTempDirectory
import javax.sql.DataSource

internal class DataSourceExtension : ParameterResolver {

    internal companion object {

        private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(tempDir)
            .setDataDirectory(tempDir.resolve("datadir"))
            .start()

        private val embeddedPostgres = embeddedPostgress(createTempDirectory("tmp_postgres").toFile())

        private val dataSource = DataSourceBuilder(mapOf(
            "DATABASE_HOST" to "localhost",
            "DATABASE_PORT" to "${embeddedPostgres.port}",
            "DATABASE_DATABASE" to "postgres",
            "DATABASE_USERNAME" to "postgres",
            "DATABASE_PASSWORD" to "postgres"
        )).build()

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    embeddedPostgres.close()
                }
            )
        }

        private val støttedeParametre = listOf(
            DataSource::class.java
        )
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return dataSource
    }
}