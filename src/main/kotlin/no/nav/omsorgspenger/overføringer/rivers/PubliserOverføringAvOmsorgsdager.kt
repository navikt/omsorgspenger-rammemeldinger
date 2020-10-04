package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.FerdigstillJournalføringForOmsorgspengerMelding.FerdigstillJournalføringForOmsorgspenger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.meldinger.leggTilLøsningPar
import org.slf4j.LoggerFactory
import kotlin.IllegalStateException

internal class PubliserOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PubliserOverføringAvOmsorgsdager::class.java)) {
    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(
                    HentPersonopplysningerMelding.Navn,
                    OverføreOmsorgsdagerBehandlingMelding.Navn,
                    HentOmsorgspengerSaksnummerMelding.Navn
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                OverføreOmsorgsdagerBehandlingMelding(it).validate()
                HentPersonopplysningerMelding(it).validate()
                HentOmsorgspengerSaksnummerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert.")
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("PubliserOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreOmsorgsdagerBehandlingMelding(packet).innhold()
        val personopplysninger = HentPersonopplysningerMelding(packet).innhold()
        val saksnummer = HentOmsorgspengerSaksnummerMelding(packet).innhold().saksnummer

        logger.info("Saksnummer ${saksnummer.values}")

        val utfall = when {
            behandling.karakteristikker.contains(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår) -> Utfall.Avslått
            behandling.overføringer.isEmpty() -> Utfall.Avslått.also { logger.warn("Oppfyller inngangsvilkår, men ingen overføringer som kan gjennomføres.") }
            else -> Utfall.Gjennomført
        }

        val overføringer = when (utfall) {
            Utfall.Gjennomført -> behandling.overføringer
            else -> overføreOmsorgsdager.ønskedeOverføringer
        }

        packet.leggTilLøsningPar(
            OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                utfall = utfall,
                begrunnelser = listOf(),
                fra = overføreOmsorgsdager.overførerFra,
                til = overføreOmsorgsdager.overførerTil,
                overføringer = overføringer,
                parter = personopplysninger.parter
            ))
        )

        packet.leggTilBehovEtter(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(Behov(
                navn = FerdigstillJournalføringForOmsorgspenger,
                input = FerdigstillJournalføringForOmsorgspengerMelding.input(
                    identitetsnummer = overføreOmsorgsdager.overførerFra,
                    journalpostIder = overføreOmsorgsdager.journalpostIder,
                    saksnummer = saksnummer.getOrElse(overføreOmsorgsdager.overførerFra,{
                        throw IllegalStateException("Mangler saksnummer for personen som overfører.")
                    })
                )
            ))
        )

        secureLogger.trace(packet.toJson())

        return true
    }
}