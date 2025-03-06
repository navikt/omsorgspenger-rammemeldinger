package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.overføringer.meldinger.OpphøreOverføringerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import org.slf4j.LoggerFactory

internal class BehandleOpphøreOverføringer(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val overføringerRepository: OverføringRepository
) : PersistentBehovssekvensPacketListener(
    steg = "BehandleOpphøreOverføringer",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(BehandleOpphøreOverføringer::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OpphøreOverføringerMelding.OpphøreOverføringer)
                OpphøreOverføringerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OpphøreOverføringerMelding.hentBehov(packet)

        val opphørteOverføringer = overføringerRepository.opphørOverføringer(
            behovssekvensId = id,
            fra = behovet.fra,
            til = behovet.til,
            fraOgMed = behovet.fraOgMed
        )

        packet.leggTilLøsningPar(
            OpphøreOverføringerMelding.løsning(opphørteOverføringer)
        )

        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}