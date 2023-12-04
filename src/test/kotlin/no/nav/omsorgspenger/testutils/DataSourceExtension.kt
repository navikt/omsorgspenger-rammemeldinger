package no.nav.omsorgspenger.testutils

import no.nav.omsorgspenger.DataSourceBuilder
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

internal class DataSourceExtension : ParameterResolver {

    private val postgreSQLContainer = PostgreSQLContainer("postgres:12.2")
        .withDatabaseName("postgresl")
        .withUsername("postgresl")
        .withPassword("postgresql")

    init {
        if(!postgreSQLContainer.isRunning) {
            postgreSQLContainer.start()
        }
    }

    private val dataSource = DataSourceBuilder(
        env = mapOf(
            "DATABASE_HOST" to postgreSQLContainer.host,
            "DATABASE_PORT" to postgreSQLContainer.firstMappedPort.toString(),
            "DATABASE_DATABASE" to postgreSQLContainer.databaseName,
            "DATABASE_USERNAME" to postgreSQLContainer.username,
            "DATABASE_PASSWORD" to postgreSQLContainer.password
        )
    ).build()

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == DataSource::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return dataSource
    }
}