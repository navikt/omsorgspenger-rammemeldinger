package no.nav.omsorgspenger.statistikk

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal interface StatistikkService {
    fun publiser(statistikkMelding: StatistikkMelding)
}

internal class KafkaStatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String = "aapen-omsorgspengerRammemeldinger-statistikk-v1") : StatistikkService {

    override fun publiser(statistikkMelding: StatistikkMelding) {
        val metadata = kafkaProducer.send(ProducerRecord(topic, statistikkMelding.toJson())).get()
        logger.info("Statistikkmelding sendt. Topic=[$topic], Offset=[${metadata.offset()}], Partition=[${metadata.partition()}].")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(KafkaStatistikkService::class.java)
    }
}