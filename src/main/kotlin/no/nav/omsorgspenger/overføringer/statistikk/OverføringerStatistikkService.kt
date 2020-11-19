package no.nav.omsorgspenger.overføringer.statistikk

import no.nav.omsorgspenger.statistikk.OverføringStatistikkMelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class OverføringerStatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String) {

    fun publiser(statistikk: OverføringStatistikkMelding) {
        kafkaProducer.send(ProducerRecord(topic, statistikk.toJson()))
    }
}