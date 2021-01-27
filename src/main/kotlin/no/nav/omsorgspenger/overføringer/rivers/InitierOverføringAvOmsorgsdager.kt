package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.correlationId
import no.nav.omsorgspenger.extensions.erFørEllerLik
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerService
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding.HentKoronaOverføringGirMeldinger
import no.nav.omsorgspenger.midlertidigalene.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.midlertidigalene.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding.VurderRelasjoner
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class InitierOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService,
    private val utvidetRettService: UtvidetRettService,
    private val midlertidigAleneService: MidlertidigAleneService,
    private val behandleMottattEtter: LocalDate,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierOverføringAvOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierOverføringAvOmsorgsdager::class.java)) {

    init {
        logger.info("BehandleMottattEtter=$behandleMottattEtter")
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.utenLøsningPåBehov(
                    HentOmsorgspengerSaksnummer,
                    HentFordelingGirMeldinger,
                    HentUtvidetRettVedtak,
                    HentMidlertidigAleneVedtak,
                    OverføreOmsorgsdagerBehandling,
                    VurderRelasjoner
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("InitierOverføringAvOmsorgsdager for $id")
        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)

        if (overføreOmsorgsdager.erBehandletIInfotrygd()) {
            logger.warn("Er behandlet i Infotrygd.")
            packet.leggTilLøsning(
                behov = OverføreOmsorgsdager,
                løsning = mapOf("melding" to "Er behandlet i Infotrygd.")
            )
            return true
        }

        require(overføreOmsorgsdager.skalBehandles())

        val periode = overføreOmsorgsdager.overordnetPeriode
        val correlationId = packet.correlationId()

        logger.info("Henter rammemeldinger & rammevedtak")
        val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        val koronaoverføringGirMeldinger = spleisetKoronaOverføringerService.hentSpleisetOverføringer(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        ).gitt

        val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        val midlertidigAleneVedtak = midlertidigAleneService.hentMidlertidigAleneVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        logger.info("legger til behov med løsninger [$HentFordelingGirMeldinger, $HentKoronaOverføringGirMeldinger, $HentUtvidetRettVedtak, $HentMidlertidigAleneVedtak]")
        logger.warn("Løsning på behov [$HentUtvidetRettVedtak,$HentMidlertidigAleneVedtak] bør flyttes til 'omsorgspenger-rammevedtak'")
        val inputHentingAvRammer = mapOf(
            "periode" to "$periode",
            "identitetsnummer" to overføreOmsorgsdager.overførerFra
        )
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                HentFordelingGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, fordelingGirMeldinger),
                HentKoronaOverføringGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, koronaoverføringGirMeldinger),
                HentUtvidetRettVedtakMelding.behovMedLøsning(inputHentingAvRammer, utvidetRettVedtak),
                HentMidlertidigAleneVedtakMelding.behovMedLøsning(inputHentingAvRammer, midlertidigAleneVedtak)
            )
        )

        logger.info("legger til behov [$HentOmsorgspengerSaksnummer, $VurderRelasjoner]")
        packet.leggTilBehov(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(
                HentOmsorgspengerSaksnummerMelding.behov(
                    HentOmsorgspengerSaksnummerMelding.BehovInput(
                        identitetsnummer = setOf(
                            overføreOmsorgsdager.overførerFra,
                            overføreOmsorgsdager.overførerTil
                        )
                    )
                ),
                VurderRelasjonerMelding.behov(
                    VurderRelasjonerMelding.BehovInput(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        til = overføreOmsorgsdager.barn
                            .map { it.identitetsnummer }
                            .plus(overføreOmsorgsdager.overførerTil)
                            .toSet()
                    )
                )
            )
        )
        return true
    }

    private fun OverføreOmsorgsdagerMelding.Behovet.erBehandletIInfotrygd() =
        mottaksdato.year == 2021 && mottaksdato.erFørEllerLik(behandleMottattEtter)
    private fun OverføreOmsorgsdagerMelding.Behovet.skalBehandles() =
        mottaksdato.isAfter(behandleMottattEtter)
}