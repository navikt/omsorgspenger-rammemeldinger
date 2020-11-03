package no.nav.omsorgspenger

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class DataSourceBuilder(env: Environment) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql://%s:%s/%s",
            env.hentRequiredEnv("DATABASE_HOST"),
            env.hentRequiredEnv("DATABASE_PORT"),
            env.hentRequiredEnv("DATABASE_DATABASE")
        )

        username = env.hentRequiredEnv("DATABASE_USERNAME")
        password = env.hentRequiredEnv("DATABASE_PASSWORD")
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        driverClassName = "org.postgresql.Driver"
    }

    internal fun build(): DataSource = kotlin.runCatching {
        HikariDataSource(hikariConfig)
    }.fold(
        onSuccess = { it },
        onFailure = { cause ->
            "Feil ved opprettelse av DataSource".let { error ->
                secureLogger.error(error, cause)
                throw IllegalStateException("$error. Se secure logs for detaljer")
            }
        }
    )

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }
}

internal fun DataSource.migrate() {
    Flyway.configure()
        .dataSource(this)
        .load()
        .migrate()
}