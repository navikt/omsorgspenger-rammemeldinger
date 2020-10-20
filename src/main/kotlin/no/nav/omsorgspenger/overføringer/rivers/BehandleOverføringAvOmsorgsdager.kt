package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.Saksreferanse
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.RoutingVurderinger.måBehandlesSomGosysJournalføringsoppgaver
import no.nav.omsorgspenger.overføringer.Vurderinger
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderInngangsvilkår
import no.nav.omsorgspenger.overføringer.meldinger.*
import no.nav.omsorgspenger.overføringer.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import no.nav.omsorgspenger.overføringer.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.meldinger.leggTilLøsningPar

import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val overføringService: OverføringService) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(
                    HentOmsorgspengerSaksnummer,
                    HentFordelingGirMeldinger,
                    HentUtvidetRettVedtak,
                    HentMidlertidigAleneVedtak
                )
                it.utenLøsningPåBehov(
                    OverføreOmsorgsdagerBehandling
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
                HentFordelingGirMeldingerMelding.validateLøsning(it)
                HentUtvidetRettVedtakMelding.validateLøsning(it)
                HentMidlertidigAleneVedtakMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("BehandleOverføringAvOmsorgsdager for $id")
        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet)
        val fordelingGirMeldinger = HentFordelingGirMeldingerMelding.hentLøsning(packet)
        val utvidetRettVedtak = HentUtvidetRettVedtakMelding.hentLøsning(packet)
        val midlertidigAleneVedtak = HentMidlertidigAleneVedtakMelding.hentLøsning(packet)

        val behandling = Behandling(
            sendtPerBrev = overføreOmsorgsdager.sendtPerBrev,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val grunnlag = Vurderinger.vurderGrunnlag(
            grunnlag = Grunnlag(
                overføreOmsorgsdager = overføreOmsorgsdager,
                utvidetRettVedtak = utvidetRettVedtak,
                fordelingGirMeldinger = fordelingGirMeldinger,
                midlertidigAleneVedtak = midlertidigAleneVedtak
            ),
            behandling = behandling
        )

        vurderInngangsvilkår(
            grunnlag = grunnlag,
            behandling = behandling
        )

        val omsorgsdagerTilgjengeligForOverføring = beregnOmsorgsdagerTilgjengeligForOverføring(
            grunnlag = grunnlag,
            behandling = behandling
        )

        logger.info("identifiserer overføringer som skal gjennomføres.")
        val overføringer = omsorgsdagerTilgjengeligForOverføring.somOverføringer(
            ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        logger.info("karakteristikker = ${behandling.karakteristikker()}")

        val måBehandlesSomGosysJournalføringsoppgaver = måBehandlesSomGosysJournalføringsoppgaver(
            behandling = behandling,
            overføreOmsorgsdager = grunnlag.overføreOmsorgsdager,
            omsorgsdagerTilgjengeligForOverføring = omsorgsdagerTilgjengeligForOverføring.mapKeys {
                it.key.periode
            }
        )

        val gjeldendeOverføringer = gjeldendeOverføringer(
            fra = Saksreferanse(
                identitetsnummer = overføreOmsorgsdager.overførerFra,
                saksnummer = saksnummer[overføreOmsorgsdager.overførerFra] ?: error("Mangler saksnummer for 'fra'")
            ),
            til = Saksreferanse(
                identitetsnummer = overføreOmsorgsdager.overførerTil,
                saksnummer = saksnummer[overføreOmsorgsdager.overførerTil] ?: error("Mangler saksnummer for 'til'")
            ),
            overføringer = overføringer,
            måBehandlesSomGosysJournalføringsoppgaver = måBehandlesSomGosysJournalføringsoppgaver
        )

        logger.info("legger til behov med løsninger [$OverføreOmsorgsdagerBehandling]")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                OverføreOmsorgsdagerBehandlingMelding.behovMedLøsning(
                    OverføreOmsorgsdagerBehandlingMelding.HeleBehandling(
                        behandling = behandling,
                        overføringer = overføringer,
                        gjeldendeOverføringer = gjeldendeOverføringer
                    )
                )
            )
        )

        if (måBehandlesSomGosysJournalføringsoppgaver) {
            logger.info("Legger til løsning på $OverføreOmsorgsdager")
            packet.leggTilLøsningPar(
                OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                    utfall = Utfall.GosysJournalføringsoppgaver,
                    gjeldendeOverføringer = mapOf(),
                    parter = emptySet()
                ))
            )
            logger.info("legger til behov [$OpprettGosysJournalføringsoppgaver]")
            packet.leggTilBehovEtter(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    OpprettGosysJournalføringsoppgaverMelding.behov(
                        OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                            identitetsnummer = overføreOmsorgsdager.overførerFra,
                            journalpostIder = overføreOmsorgsdager.journalpostIder
                        )
                    )
                )
            )
        } else {
            logger.info("legger til behov [$HentPersonopplysninger]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    HentPersonopplysningerMelding.behov(
                        HentPersonopplysningerMelding.BehovInput(
                            identitetsnummer = gjeldendeOverføringer.berørteIdentitetsnummer()
                        )
                    )
                )
            )
        }

        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert. https://github.com/navikt/omsorgspenger-rammemeldinger/issues/12")
    }

    private fun gjeldendeOverføringer(
        fra: Saksreferanse,
        til: Saksreferanse,
        overføringer: List<Overføring>,
        måBehandlesSomGosysJournalføringsoppgaver: Boolean) = when (måBehandlesSomGosysJournalføringsoppgaver) {
        true -> mapOf(
            fra.identitetsnummer to GjeldendeOverføringer(gitt = overføringer.gitt(til), saksnummer = fra.saksnummer),
            til.identitetsnummer to GjeldendeOverføringer(fått = overføringer.fått(fra), saksnummer = til.saksnummer)
        )
        false -> overføringService.gjennomførOverføringer(
            fra = fra,
            til = til,
            overføringer = overføringer
        )
    }
}