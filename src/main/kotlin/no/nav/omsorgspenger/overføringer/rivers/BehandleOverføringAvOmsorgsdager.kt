package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenFor
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenRepository
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderGrunnlag
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
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import no.nav.omsorgspenger.saksnummer.identitetsnummer

import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val gjennomførOverføringService: GjennomførOverføringService,
    private val saksnummerRepository: SaksnummerRepository,
    private val aleneOmOmsorgenRepository: AleneOmOmsorgenRepository) : BehovssekvensPacketListener(
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
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).also {
            require(it.containsKey(overføreOmsorgsdager.overførerFra)) { "Mangler saksnummer for 'overførerFra'"}
            require(it.containsKey(overføreOmsorgsdager.overførerTil)) { "Mangler saksnummer for 'overførerTil'"}
        }
        val fordelingGirMeldinger = HentFordelingGirMeldingerMelding.hentLøsning(packet)
        val utvidetRettVedtak = HentUtvidetRettVedtakMelding.hentLøsning(packet)
        val midlertidigAleneVedtak = HentMidlertidigAleneVedtakMelding.hentLøsning(packet)

        saksnummerRepository.lagreMapping(saksnummer)
        lagreAleneOmOmsorgen(
            behovssekvensId = id,
            saksnummer = saksnummer.getValue(overføreOmsorgsdager.overførerFra),
            overføreOmsorgsdager = overføreOmsorgsdager
        )

        val behandling = Behandling(
            sendtPerBrev = overføreOmsorgsdager.sendtPerBrev,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val grunnlag = vurderGrunnlag(
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
        val overføringer = omsorgsdagerTilgjengeligForOverføring.somNyeOverføringer(
            ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        logger.info("karakteristikker = ${behandling.karakteristikker()}")

        val måBehandlesSomGosysJournalføringsoppgaver = behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett()
        val avslag = behandling.avslag() || måBehandlesSomGosysJournalføringsoppgaver || overføringer.fjernOverføringerUtenDager().isEmpty()

        logger.info("Avlsag=$avslag")

        val gjeldendeOverføringer = when (avslag) {
            true -> overføringer.somAvslåtteGjeldendeOverføringer(
                fra = saksnummer.getValue(overføreOmsorgsdager.overførerFra),
                til = saksnummer.getValue(overføreOmsorgsdager.overførerTil)
            )
            false -> gjennomførOverføringService.gjennomførOverføringer(
                fra = saksnummer.getValue(overføreOmsorgsdager.overførerFra),
                til = saksnummer.getValue(overføreOmsorgsdager.overførerTil),
                overføringer = overføringer
            )
        }

        val alleSaksnummer = when (avslag) {
            true -> saksnummer
            false -> saksnummerRepository.hentSisteMappingFor(
                saksnummer = gjeldendeOverføringer.saksnummer()
            ).plus(saksnummer)
        }

        val alleIdentitetsnummer = alleSaksnummer.identitetsnummer()

        logger.info("legger til behov med løsninger [$OverføreOmsorgsdagerBehandling]")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                OverføreOmsorgsdagerBehandlingMelding.behovMedLøsning(
                    løsning = OverføreOmsorgsdagerBehandlingMelding.HeleBehandling(
                        behandling = behandling,
                        overføringer = overføringer,
                        gjeldendeOverføringer = gjeldendeOverføringer,
                        saksnummer = alleSaksnummer
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
                    personopplysninger = mapOf(),
                    saksnummer = mapOf()
                ))
            )
            logger.info("legger til behov [$OpprettGosysJournalføringsoppgaver]")
            packet.leggTilBehovEtter(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    OpprettGosysJournalføringsoppgaverMelding.behov(
                        OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                            fra = overføreOmsorgsdager.overførerFra,
                            alleIdentitetsnummer = alleIdentitetsnummer,
                            journalpostIder = overføreOmsorgsdager.journalpostIder
                        )
                    )
                )
            )
            secureLogger.info("SuccessPacket=${packet.toJson()}")
        } else {
            logger.info("legger til behov [$HentPersonopplysninger]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    HentPersonopplysningerMelding.behov(
                        HentPersonopplysningerMelding.BehovInput(
                            identitetsnummer = alleIdentitetsnummer
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

    private fun lagreAleneOmOmsorgen(
        behovssekvensId: BehovssekvensId,
        saksnummer: Saksnummer,
        overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet)  {
        aleneOmOmsorgenRepository.lagre(
            saksnummer = saksnummer,
            behovssekvensId = behovssekvensId,
            registreresIForbindelseMed = AleneOmOmsorgenRepository.RegistreresIForbindelseMed.Overføring,
            aleneOmOmsorgenFor = overføreOmsorgsdager.barn.filter { it.aleneOmOmsorgen }.mapNotNull {
                /**
                 * Lagrer nå alene om omsorgen ut året barnet fyller 18 uavhengig av det utvidet rett eller ikke.
                 * Dette for å unngå ev. problemer om man skulle få utvidet rett for barnet senere.
                 */
                val aleneOmOmsorgenTilOgMed = it.fødselsdato.plusYears(18).sisteDagIÅret()
                when (overføreOmsorgsdager.mottaksdato.isAfter(aleneOmOmsorgenTilOgMed)) {
                    true -> null
                    false -> AleneOmOmsorgenFor(
                        identitetsnummer = it.identitetsnummer,
                        fødselsdato = it.fødselsdato,
                        aleneOmOmsorgenI = Periode(
                            fom = overføreOmsorgsdager.mottaksdato,
                            tom = aleneOmOmsorgenTilOgMed
                        )
                    )
                }
            }.toSet()
        )
    }
}