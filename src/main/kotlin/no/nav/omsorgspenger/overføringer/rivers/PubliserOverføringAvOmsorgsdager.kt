package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.MockLøsning.mockLøsning
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import org.slf4j.LoggerFactory

internal class PubliserOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.harLøsningPåBehov(
                    HentPersonopplysningerMelding.Navn,
                    OverføreOmsorgsdagerBehandlingMelding.Navn
                )
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
                OverføreOmsorgsdagerBehandlingMelding(it).validate()
                HentPersonopplysningerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()

        logger.info("$id -> BehandleOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold()
        val behandling = OverføreOmsorgsdagerBehandlingMelding(packet).innhold()
        val personopplysninger = HentPersonopplysningerMelding(packet).innhold()


        packet.leggTilBehov(
            aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
            behov = arrayOf(Behov(
                navn = FerdigstillJournalføringForOmsorgspengerMelding.Navn,
                input = FerdigstillJournalføringForOmsorgspengerMelding.input(
                    identitetsnummer = overføreOmsorgsdager.overførerFra,
                    journalpostIder = overføreOmsorgsdager.journalpostIder
                )
            ))
        )

        val utfall = when {
            behandling.karakteristikker.contains(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår) -> Utfall.Avslått
            behandling.overføringer.isEmpty() -> Utfall.Avslått.also { logger.warn("Oppfyller inngangsvilkår, men ingen overføringer som kan gjennomføres.") }
            else -> Utfall.Gjennomført
        }

        val overføringer = when (utfall) {
            Utfall.Gjennomført -> behandling.overføringer
            else -> overføreOmsorgsdager.ønskedeOverføringer()
        }


        packet.leggTilLøsning(OverføreOmsorgsdagerMelding.Navn, mockLøsning(
            utfall = utfall,
            begrunnelser = listOf(),
            fra = overføreOmsorgsdager.overførerFra,
            til = overføreOmsorgsdager.overførerTil,
            overføringer = overføringer,
            parter = personopplysninger.parter
        ))

        println(packet.toJson())

        context.sendMedId(packet)

    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PubliserOverføringAvOmsorgsdager::class.java)
    }
}