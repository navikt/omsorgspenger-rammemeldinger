package no.nav.omsorgspenger.formidling

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

internal interface FormidlingService {
    fun sendMeldingsbestillinger(meldingsbestillinger: List<Meldingsbestilling>)
}

internal class KafkaFormidlingService(
    private val kafkaProducer: KafkaProducer<String, String>,
    private val topic: String = "privat-k9-dokumenthendelse") : FormidlingService {

    override fun sendMeldingsbestillinger(meldingsbestillinger: List<Meldingsbestilling>) {
        meldingsbestillinger.forEach { meldingsbestilling ->
            val (key, value) = meldingsbestilling.keyValue
            secureLogger.info("Meldingsbestilling=$value")
            val recordMetaData = kafkaProducer.send(ProducerRecord(topic, key, value)).get()
            logger.info("Sendt til $recordMetaData")
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(KafkaFormidlingService::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }
}