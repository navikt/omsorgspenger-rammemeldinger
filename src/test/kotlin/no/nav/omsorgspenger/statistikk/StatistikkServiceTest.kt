package no.nav.omsorgspenger.statistikk

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

internal class StatistikkServiceTest {
    private val kafkaProducer = mockk<KafkaProducer<String, String>>()
    private val topic = "topic"

    private val statistikkService = StatistikkService(
            kafkaProducer = kafkaProducer,
            topic = topic,
            enabled = true
    )

    @BeforeEach
    internal fun setUp() {
        clearMocks(kafkaProducer)
    }

    @Test
    internal fun name() {
        val meldinger = mutableListOf<ProducerRecord<String, String>>()

        every { kafkaProducer.send(capture(meldinger)) }.returns(CompletableFuture.completedFuture(null))

        val melding = StatistikkMelding(
                saksnummer = "",
                behandlingId = "",
                mottattDato = LocalDate.parse("2020-01-01"),
                registrertDato = LocalDate.parse("2020-01-01"),
                behandlingType = "",
                behandlingStatus = "",
                funksjonellTid = OffsetDateTime.now(),
                aktorId = "12345678",
                tekniskTid = OffsetDateTime.now(),
        )
        statistikkService.publiser(melding)

        assertThat(meldinger.map { it.topic() }).containsExactly(topic)
        assertThat(meldinger.map { StatistikkMelding.fromJson(it.value()) }).containsExactly(melding)
    }
}