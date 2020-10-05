package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
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
                    OverføreOmsorgsdagerBehandling,
                    HentPersonopplysninger,
                    HentOmsorgspengerSaksnummer
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                OverføreOmsorgsdagerBehandlingMelding.validateLøsning(it)
                HentPersonopplysningerMelding.validateLøsning(it)
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert. https://github.com/navikt/omsorgspenger-rammemeldinger/issues/12")
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("PubliserOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreOmsorgsdagerBehandlingMelding.hentLøsning(packet)
        val parter = HentPersonopplysningerMelding.hentLøsning(packet)
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet)

        logger.info("Saksnummer ${saksnummer.values}")

        val utfall = when {
            behandling.karakteristikker.contains(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår) -> Utfall.Avslått
            behandling.overføringer.isEmpty() -> Utfall.Avslått.also { logger.warn("Oppfyller inngangsvilkår, men ingen overføringer som kan gjennomføres.") }
            else -> Utfall.Gjennomført
        }

        val overføringer = when (utfall) {
            Utfall.Gjennomført -> behandling.overføringer
            else -> listOf(Overføring(
                antallDager = overføreOmsorgsdager.omsorgsdagerÅOverføre,
                periode = behandling.periode
            ))
        }

        packet.leggTilLøsningPar(
            OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                utfall = utfall,
                begrunnelser = listOf(),
                fra = overføreOmsorgsdager.overførerFra,
                til = overføreOmsorgsdager.overførerTil,
                overføringer = overføringer,
                parter = parter
            ))
        )

        packet.leggTilBehovEtter(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(
                FerdigstillJournalføringForOmsorgspengerMelding.behov(
                    FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        journalpostIder = overføreOmsorgsdager.journalpostIder,
                        saksnummer = saksnummer.getOrElse(overføreOmsorgsdager.overførerFra,{
                            throw IllegalStateException("Mangler saksnummer for personen som overfører.")
                        })
                    )
                )
            )
        )

        // TODO: Send bestilling på formidling https://github.com/navikt/omsorgspenger-rammemeldinger/issues/14
        // TODO: Send info om saksstatistikk https://github.com/navikt/omsorgspenger-rammemeldinger/issues/15

        secureLogger.trace(packet.toJson())

        return true
    }
}