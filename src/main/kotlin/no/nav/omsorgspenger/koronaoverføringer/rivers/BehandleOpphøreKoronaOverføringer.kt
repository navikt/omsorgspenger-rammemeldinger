package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.koronaoverføringer.db.KoronaoverføringRepository
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OpphøreKoronaOverføringerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import org.slf4j.LoggerFactory

internal class BehandleOpphøreKoronaOverføringer(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val koronaoverføringRepository: KoronaoverføringRepository
) : PersistentBehovssekvensPacketListener(
    steg = "BehandleOpphøreKoronaOverføringer",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(BehandleOpphøreKoronaOverføringer::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OpphøreKoronaOverføringerMelding.OpphøreKoronaOverføringer)
                OpphøreKoronaOverføringerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OpphøreKoronaOverføringerMelding.hentBehov(packet)
        val opphørteOverføringer = koronaoverføringRepository.opphørOverføringer(
            fra = behovet.fra,
            til = behovet.til,
            fraOgMed = behovet.fraOgMed
        )

        packet.leggTilLøsningPar(
            OpphøreKoronaOverføringerMelding.løsning(opphørteOverføringer)
        )

        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}