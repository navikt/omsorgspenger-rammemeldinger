package no.nav.omsorgspenger.testutils

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenResponse
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.flywaydb.core.Flyway
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource

internal fun TestApplicationContextBuilder(
    dataSource: DataSource
) = ApplicationContext.Builder(
    accessTokenClient = mockk<AccessTokenClient>().also {
        every { it.getAccessToken(any()) }.returns(AccessTokenResponse(accessToken = "foo", expiresIn = 1000, tokenType = "Bearer"))
    },
    kafkaProducer = mockk<KafkaProducer<String, String>>().also {
        every { it.send(any()) }.returns(CompletableFuture.completedFuture(RecordMetadata(
            TopicPartition("foo", 1),
            1L,
            1L,
            System.currentTimeMillis(),
            1L,
            1,
            1
        )))
    },
    omsorgspengerInfotrygdRammevedtakGateway = mockk<OmsorgspengerInfotrygdRammevedtakGateway>().also {
        every { it.hent(any(), any(), any())}.returns(listOf())
        coEvery { it.check() }.returns(Healthy("OmsorgspengerInfotrygdRammevedtakGateway", "Mock helsesjekk OK!"))
    },
    dataSource = dataSource
)

internal fun DataSource.cleanAndMigrate() = this.also {
    Flyway
        .configure()
        .dataSource(this)
        .load()
        .also {
            it.clean()
            it.migrate()
        }
}