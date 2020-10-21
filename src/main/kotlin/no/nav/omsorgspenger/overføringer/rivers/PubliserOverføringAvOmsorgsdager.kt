package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Fordmidling.opprettMeldingsBestillinger
import no.nav.omsorgspenger.overføringer.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.meldinger.leggTilLøsningPar
import org.slf4j.LoggerFactory

internal class PubliserOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PubliserOverføringAvOmsorgsdager::class.java)) {
    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(
                    OverføreOmsorgsdagerBehandling,
                    HentPersonopplysninger
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                OverføreOmsorgsdagerBehandlingMelding.validateLøsning(it)
                HentPersonopplysningerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("PubliserOverføringAvOmsorgsdager for $id")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreOmsorgsdagerBehandlingMelding.hentLøsning(packet)
        val personopplysninger = HentPersonopplysningerMelding.hentLøsning(packet)

        val utfall = when {
            behandling.oppfyllerIkkeInngangsvilkår() -> Utfall.Avslått
            behandling.overføringer.isEmpty() -> Utfall.Avslått
            else -> Utfall.Gjennomført
        }

        packet.leggTilLøsningPar(
            OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                utfall = utfall,
                gjeldendeOverføringer = behandling.gjeldendeOverføringer,
                personopplysninger = personopplysninger
            ))
        )

        val saksnummer = behandling.gjeldendeOverføringer.entries.first {
            it.key == overføreOmsorgsdager.overførerFra
        }.value.saksnummer

        packet.leggTilBehovEtter(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(
                FerdigstillJournalføringForOmsorgspengerMelding.behov(
                    FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        journalpostIder = overføreOmsorgsdager.journalpostIder,
                        saksnummer = saksnummer
                    )
                )
            )
        )
        
        opprettMeldingsBestillinger(
            behovssekvensId = id,
            personopplysninger = personopplysninger,
            saksnummer = behandling.gjeldendeOverføringer.entries.map { it.key to it.value.saksnummer }.toMap(),
            overføringFra = overføreOmsorgsdager.overførerFra,
            overføringTil = overføreOmsorgsdager.overførerTil,
            overføringer = behandling.overføringer,
            mottaksdato = overføreOmsorgsdager.mottaksdato,
            måSendesSomBrev = behandling.måBesvaresPerBrev(),
            varighetPåOverføringUtledetFraBarnMedUtvidetRett = behandling.varighetPåOverføringUtledetFraBarnMedUtvidetRett()
        ).forEach {
            logger.info(it.keyValue.second)
        }

        // TODO: Send bestilling på formidling https://github.com/navikt/omsorgspenger-rammemeldinger/issues/14
        // TODO: Send info om saksstatistikk https://github.com/navikt/omsorgspenger-rammemeldinger/issues/15

        secureLogger.trace(packet.toJson())

        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert. https://github.com/navikt/omsorgspenger-rammemeldinger/issues/12")
    }
}