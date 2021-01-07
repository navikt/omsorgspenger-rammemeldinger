package no.nav.omsorgspenger.koronaoverføringer.rivers

import KoronaoverføringRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.koronaoverføringer.Behandling
import no.nav.omsorgspenger.koronaoverføringer.Beregninger
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag.Companion.vurdert
import no.nav.omsorgspenger.koronaoverføringer.ManuellVurdering
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOverføringGirMeldingerMelding
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerInput
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding.Companion.HentPersonopplysninger
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.saksnummer.SaksnummerRepository
import no.nav.omsorgspenger.saksnummer.identitetsnummer
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding
import org.slf4j.LoggerFactory

internal class BehandleOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val koronaoverføringRepository: KoronaoverføringRepository,
    private val saksnummerRepository: SaksnummerRepository
) : PersistentBehovssekvensPacketListener(
    steg = "BehandleOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(BehandleOverføreKoronaOmsorgsdager::class.java)) {

    private val aktueltBehov = OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(aktueltBehov)
                it.harLøsningPåBehov(HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer)
                it.utenLøsningPåBehov(HentPersonopplysninger)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
                HentFordelingGirMeldingerMelding.validateLøsning(it)
                HentOverføringGirMeldingerMelding.validateLøsning(it)
                HentKoronaOverføringGirMeldingerMelding.validateLøsning(it)
                HentUtvidetRettVedtakMelding.validateLøsning(it)
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
                VurderRelasjonerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)
        val fordelingGirMeldinger = HentFordelingGirMeldingerMelding.hentLøsning(packet)
        val overføringGirMeldinger = HentOverføringGirMeldingerMelding.hentLøsning(packet)
        val koronaOverføringGirMeldinger = HentKoronaOverføringGirMeldingerMelding.hentLøsning(packet)
        val utvidetRettVedtak = HentUtvidetRettVedtakMelding.hentLøsning(packet)

        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).also {
            require(it.containsKey(behovet.fra)) { "Mangler saksnummer for 'fra'"}
            require(it.containsKey(behovet.til)) { "Mangler saksnummer for 'til'"}
        }

        saksnummerRepository.lagreMapping(saksnummer)
        // TODO: Skal vi også lagre alene om omsorgen?

        val fraSaksnummer = saksnummer.getValue(behovet.fra)
        val tilSaksnummer = saksnummer.getValue(behovet.til)

        val behandling = Behandling(behovet)

        val relasjoner = VurderRelasjonerMelding.hentLøsning(packet)

        val grunnlag = Grunnlag(
            behovet = behovet,
            utvidetRett = utvidetRettVedtak,
            fordelinger = fordelingGirMeldinger,
            overføringer = overføringGirMeldinger,
            relasjoner = relasjoner,
            koronaoverføringer = koronaOverføringGirMeldinger
        ).vurdert(behandling)

        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = behandling,
            grunnlag = grunnlag
        )

        val måVurderesManuelt = ManuellVurdering.måVurderesManuelt(
            behandling = behandling,
            grunnlag = grunnlag,
            dagerTilgjengeligForOverføring = dagerTilgjengeligForOverføring
        )

        val overføring = NyOverføring(
            periode = behandling.periode,
            antallDager = when (dagerTilgjengeligForOverføring >= behovet.omsorgsdagerÅOverføre) {
                true -> behovet.omsorgsdagerÅOverføre
                false -> dagerTilgjengeligForOverføring
            }
        )

        val skalGjennomføres = !måVurderesManuelt && overføring.skalGjennomføres
        logger.info("SkalGjennomføres=$skalGjennomføres")

        val gjennomførtOverføringer = when (skalGjennomføres) {
            true -> koronaoverføringRepository.gjennomførOverføringer(
                behovssekvensId = id,
                fra = fraSaksnummer,
                til = tilSaksnummer,
                lovanvendelser = behandling.lovanvendelser,
                antallDagerØnsketOverført = behovet.omsorgsdagerÅOverføre,
                overføringer = listOf(overføring)
            ).kunGjeldendeOverføringerForBerørteParter().also {
                behandling.gjennomførtOverføringer = true
            }
            false -> overføring.somAvslått(
                behovssekvensId = id,
                fra = fraSaksnummer,
                til = tilSaksnummer,
                antallDagerØnsketOverført = behovet.omsorgsdagerÅOverføre
            )
        }

        val alleSaksnummerMapping = when (skalGjennomføres) {
            true -> saksnummerRepository.hentSisteMappingFor(
                saksnummer = gjennomførtOverføringer.alleSaksnummer
            )
            false -> saksnummer
        }

        packet.leggTilBehovMedLøsninger(
            aktueltBehov = aktueltBehov,
            behovMedLøsninger = arrayOf(OverføreKoronaOmsorgsdagerBehandlingMelding.behovMedLøsning(
                løsning = OverføreKoronaOmsorgsdagerBehandlingMelding.HeleBehandling(
                    fraSaksnummer = fraSaksnummer,
                    tilSaksnummer = tilSaksnummer,
                    overføringer = listOf(overføring),
                    alleSaksnummerMapping = alleSaksnummerMapping,
                    gjeldendeOverføringer = gjennomførtOverføringer.gjeldendeOverføringer,
                    behandling = behandling
                )
            ))
        )

        if (måVurderesManuelt) {
            packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
                OverføreKoronaOmsorgsdagerMelding.Løsningen.GosysJournalføringsoppgaver
            ))
            packet.leggTilBehovEtter(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(behovet.somOpprettGosysJournalføringsoppgaverBehov())
            )
            logger.warn("Legger til behov $OpprettGosysJournalføringsoppgaver")
            secureLogger.info("SuccessPacket=${packet.toJson()}")
        } else {
            logger.info("legger til behov $HentPersonopplysninger")
            packet.leggTilBehov(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(
                    OverføreKoronaOmsorgsdagerPersonopplysningerMelding.behov(
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