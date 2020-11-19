package no.nav.omsorgspenger.overføringer.statistikk

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.statistikk.OverføringStatistikkMelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

internal class OverføringerStatistikkServiceTest {
    private val kafkaProducer = mockk<KafkaProducer<String, String>>()
    private val topic = "topic"

    private val overføringerStatistikkService = OverføringerStatistikkService(
            kafkaProducer = kafkaProducer,
            topic = topic
    )

    @BeforeEach
    internal fun setUp() {
        clearMocks(kafkaProducer)
    }

    @Test
    internal fun name() {
        val meldinger = mutableListOf<ProducerRecord<String, String>>()

        every { kafkaProducer.send(capture(meldinger)) }.returns(CompletableFuture.completedFuture(null))

        overføringerStatistikkService.publiser(OverføringStatistikkMelding(
                saksnummer = "",
                behandlingId = "",
                mottattDato = LocalDate.parse("2020-01-01"),
                registrertDato = LocalDate.parse("2020-01-01"),
                behandlingType = "",
                behandlingStatus = "",
                funksjonellTid = OffsetDateTime.now(),
                aktorId = "12345678",
                tekniskTid = OffsetDateTime.now(),
        ))

        assertThat(meldinger.map { it.topic() }).containsExactly(topic)
    }
}