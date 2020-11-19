package no.nav.omsorgspenger.midlertidigalene.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import org.slf4j.LoggerFactory

internal class InitierMidlertidigAlene(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierMidlertidigAlene",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierMidlertidigAlene::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(MidlertidigAleneMelding.MidlertidigAlene)
                MidlertidigAleneMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        MidlertidigAleneMelding.hentBehov(packet)
        logger.warn("Håndtering av behov ${MidlertidigAleneMelding.MidlertidigAlene} bør flyttes til 'omsorgspenger-rammevedtak'")
        logger.info("Støtter ikke å løse behovet ${MidlertidigAleneMelding.MidlertidigAlene} enda.")
        return false
    }
}