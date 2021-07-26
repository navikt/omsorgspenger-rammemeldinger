package no.nav.omsorgspenger.statistikk

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.Enhetstype
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

internal class StatistikkServiceTest {
    private val kafkaProducer = mockk<KafkaProducer<String, String>>()
    private val topic = "topic"

    private val statistikkService = KafkaStatistikkService(
            kafkaProducer = kafkaProducer,
            topic = topic
    )

    @BeforeEach
    internal fun setUp() {
        clearMocks(kafkaProducer)
    }

    @Test
    fun `Sending av statistikkmeldinger`() {
        val meldinger = mutableListOf<ProducerRecord<String, String>>()

        every { kafkaProducer.send(capture(meldinger)) }.returns(CompletableFuture.completedFuture(RecordMetadata(
            TopicPartition("topic", 1),
            1L,
            2L,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            1,
            2
        )))

        val melding = StatistikkMelding.instance(
            saksnummer = "1",
            behovssekvensId = "2",
            mottaksdato = LocalDate.parse("2020-01-01"),
            registreringsdato = LocalDate.parse("2020-01-01"),
            behandlingType = "type",
            behandlingResultat = "resulat",
            undertype = "under",
            mottatt = ZonedDateTime.now(),
                aktørId = "12345678",
                enhet = Enhet(enhetstype = Enhetstype.VANLIG, enhetsnummer = "1234")
        )
        statistikkService.publiser(melding)

        assertThat(meldinger.map { it.topic() }).containsExactly(topic)
        assertThat(meldinger.map { StatistikkMelding.fromJson(it.value()) }).containsExactly(melding)
    }
}