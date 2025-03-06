package no.nav.omsorgspenger.fordelinger.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.fordelinger.meldinger.FordelingAvOmsorgsdagerMelding
import no.nav.omsorgspenger.fordelinger.meldinger.FordelingAvOmsorgsdagerMelding.FordelingAvOmsorgsdager
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding
import org.slf4j.LoggerFactory

internal class InitierFordelingAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierFordeleOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierFordelingAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(FordelingAvOmsorgsdager)
                FordelingAvOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage) : Boolean{

        val behovet = FordelingAvOmsorgsdagerMelding.hentBehov(packet)

        packet.leggTilLøsningPar(FordelingAvOmsorgsdagerMelding.løsning(FordelingAvOmsorgsdagerMelding.Løsningen()))

        logger.info("Legger til behovet ${OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver}")

        packet.leggTilBehovEtter(FordelingAvOmsorgsdager, behovet.somOpprettGosysJournalføringsoppgaverBehov())

        return true
    }
}