package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.overføringer.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.MockLøsning.mockLøsning
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderInngangsvilkår
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderOmsorgenFor
import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.harLøsningPåBehov(
                    HentPersonopplysningerMelding.Navn,
                )
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, rapidContext: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()

        logger.info("$id -> BehandleOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold()


        packet.leggTilLøsning(OverføreOmsorgsdagerMelding.Navn, mockLøsning(
            utfall = "Gjennomført",
            begrunnelser = listOf(),
            fra = overføreOmsorgsdager.overførerFra,
            til = overføreOmsorgsdager.overførerTil,
            omsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        ))

        rapidContext.sendMedId(packet)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)
    }

}