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
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerVurderinger
import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager (rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.harLøsningPåBehov(
                    HentOmsorgspengerSaksnummerMelding.Navn,
                    HentUtvidetRettVedtakMelding.Navn,
                    HentFordelingGirMeldingerMelding.Navn
                )
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
                HentOmsorgspengerSaksnummerMelding(it).validate()
                HentUtvidetRettVedtakMelding(it).validate()
                HentFordelingGirMeldingerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        val lovanvendelseBuilder = "" // TODO

        val grunnlag = OverføreOmsorgsdagerVurderinger.vurderOmsorgenFor(
            grunnlag = OverføreOmsorgsdagerGrunnlag(
                overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold(),
                utvidetRettVedtak = HentUtvidetRettVedtakMelding(packet).innhold(),
                fordelingGirMeldinger = HentFordelingGirMeldingerMelding(packet).innhold()
            ),
            lovanvendelseBuilder = lovanvendelseBuilder
        )

        OverføreOmsorgsdagerVurderinger.vurderInngangsvilkår(
            grunnlag = grunnlag,
            lovanvendelseBuilder = lovanvendelseBuilder
        )


        logger.info("$id -> BehandleOverføringAvOmsorgsdager -> Legger til løsning")

        packet.leggTilLøsning(OverføreOmsorgsdagerMelding.Navn, mockLøsning(
            utfall = "Gjennomført",
            begrunnelser = listOf(),
            fra = overføreOmsorgsdager.overførerFra(),
            til = overføreOmsorgsdager.overførerTil(),
            omsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre()
        ))

        context.sendMedId(packet)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)
    }

}