package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgen
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenService
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderGrunnlag
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderInngangsvilkår
import no.nav.omsorgspenger.overføringer.meldinger.*
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding
import no.nav.omsorgspenger.midlertidigalene.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.midlertidigalene.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerInput
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding.Companion.HentPersonopplysninger
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding.VurderRelasjoner
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import no.nav.omsorgspenger.saksnummer.identitetsnummer

import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val gjennomførOverføringService: GjennomførOverføringService,
    private val saksnummerRepository: SaksnummerRepository,
    private val aleneOmOmsorgenService: AleneOmOmsorgenService,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "BehandleOverføringAvOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(
                    HentOmsorgspengerSaksnummer,
                    HentFordelingGirMeldinger,
                    HentUtvidetRettVedtak,
                    HentMidlertidigAleneVedtak,
                    VurderRelasjoner
                )
                it.utenLøsningPåBehov(
                    OverføreOmsorgsdagerBehandling
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
                HentFordelingGirMeldingerMelding.validateLøsning(it)
                HentKoronaOverføringGirMeldingerMelding.validateLøsning(it)
                HentUtvidetRettVedtakMelding.validateLøsning(it)
                HentMidlertidigAleneVedtakMelding.validateLøsning(it)
                VurderRelasjonerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("BehandleOverføringAvOmsorgsdager for $id")
        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val fraTilSaksnummerMapping = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).also {
            require(it.containsKey(overføreOmsorgsdager.overførerFra)) { "Mangler saksnummer for 'overførerFra'"}
            require(it.containsKey(overføreOmsorgsdager.overførerTil)) { "Mangler saksnummer for 'overførerTil'"}
        }
        val fordelingGirMeldinger = HentFordelingGirMeldingerMelding.hentLøsning(packet)
        val koronaOverføringer = HentKoronaOverføringGirMeldingerMelding.hentLøsning(packet)
        val utvidetRettVedtak = HentUtvidetRettVedtakMelding.hentLøsning(packet)
        val midlertidigAleneVedtak = HentMidlertidigAleneVedtakMelding.hentLøsning(packet)

        saksnummerRepository.lagreMapping(fraTilSaksnummerMapping)

        val behandling = Behandling(
            sendtPerBrev = overføreOmsorgsdager.sendtPerBrev,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val relasjoner = VurderRelasjonerMelding.hentLøsning(packet)

        val grunnlag = vurderGrunnlag(
            grunnlag = Grunnlag(
                overføreOmsorgsdager = overføreOmsorgsdager,
                utvidetRettVedtak = utvidetRettVedtak,
                fordelingGirMeldinger = fordelingGirMeldinger,
                midlertidigAleneVedtak = midlertidigAleneVedtak,
                koronaOverføringer = koronaOverføringer
            ),
            relasjoner = relasjoner,
            behandling = behandling
        )

        vurderInngangsvilkår(
            grunnlag = grunnlag,
            behandling = behandling
        )

        aleneOmOmsorgenService.lagreIForbindelseMedOverføring(
            behovssekvensId = id,
            saksnummer = fraTilSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra),
            dato = grunnlag.overføreOmsorgsdager.mottaksdato,
            aleneOmOmsorgenFor = grunnlag.overføreOmsorgsdager.barn.filter { it.aleneOmOmsorgen }.map { AleneOmOmsorgen.Barn(
                identitetsnummer = it.identitetsnummer,
                fødselsdato = it.fødselsdato,
            )}
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

        val måBehandlesSomGosysJournalføringsoppgaver = behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett() || !overføreOmsorgsdager.jobberINorge
        val avslag = behandling.avslag() || måBehandlesSomGosysJournalføringsoppgaver || overføringer.fjernOverføringerUtenDager().isEmpty()

        logger.info("Avslag=$avslag")

        val gjennomførtOverføringer = when (avslag) {
            true -> overføringer.somAvslått(
                fra = fraTilSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra),
                til = fraTilSaksnummerMapping.getValue(overføreOmsorgsdager.overførerTil),
                antallDagerØnsketOverført = overføreOmsorgsdager.omsorgsdagerÅOverføre
            )
            false -> gjennomførOverføringService.gjennomførOverføringer(
                fra = fraTilSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra),
                til = fraTilSaksnummerMapping.getValue(overføreOmsorgsdager.overførerTil),
                overføringer = overføringer,
                behovssekvensId = id,
                lovanvendelser = behandling.lovanvendelser,
                antallDagerØnsketOverført = overføreOmsorgsdager.omsorgsdagerÅOverføre
            )
        }

        val alleSaksnummerMapping = when (avslag) {
            true -> fraTilSaksnummerMapping
            false -> saksnummerRepository.hentSisteMappingFor(
                saksnummer = gjennomførtOverføringer.alleSaksnummer
            )
        }

        logger.info("legger til behov med løsninger [$OverføreOmsorgsdagerBehandling]")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                OverføreOmsorgsdagerBehandlingMelding.behovMedLøsning(
                    løsning = OverføreOmsorgsdagerBehandlingMelding.HeleBehandling(
                        behandling = behandling,
                        overføringer = overføringer,
                        gjeldendeOverføringer = gjennomførtOverføringer.gjeldendeOverføringer,
                        alleSaksnummerMapping = alleSaksnummerMapping,
                        berørteSaksnummer = gjennomførtOverføringer.berørteSaksnummer
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
                    alleSaksnummerMapping = mapOf()
                ))
            )
            logger.info("legger til behov [$OpprettGosysJournalføringsoppgaver]")
            packet.leggTilBehovEtter(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    OpprettGosysJournalføringsoppgaverMelding.behov(
                        OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                            identitetsnummer = overføreOmsorgsdager.overførerFra,
                            berørteIdentitetsnummer = setOf(overføreOmsorgsdager.overførerTil),
                            journalpostIder = overføreOmsorgsdager.journalpostIder,
                            journalpostType = "OverføreOmsorgsdager"
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
                    OverføreOmsorgsdagerPersonopplysningerMelding.behov(
                        HentPersonopplysningerInput(
                            identitetsnummer = alleSaksnummerMapping.identitetsnummer()
                        )
                    )
                )
            )
        }

        return true
    }
}