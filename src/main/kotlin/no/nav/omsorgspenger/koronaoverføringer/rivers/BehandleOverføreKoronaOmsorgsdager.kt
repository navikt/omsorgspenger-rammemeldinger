package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.koronaoverføringer.Behandling
import no.nav.omsorgspenger.koronaoverføringer.Beregninger
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag.Companion.vurdert
import no.nav.omsorgspenger.koronaoverføringer.ManuellVurdering
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.saksnummer.identitetsnummer
import org.slf4j.LoggerFactory

internal class BehandleOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
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
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)

        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).also {
            require(it.containsKey(behovet.fra)) { "Mangler saksnummer for 'fra'"}
            require(it.containsKey(behovet.til)) { "Mangler saksnummer for 'til'"}
        }

        val behandling = Behandling(behovet)

        val grunnlag = Grunnlag(
            overføringen = behovet,
            utvidetRett = listOf(), // TODO
            fordelinger = listOf(), // TODO
            overføringer = listOf(), // TODO
            koronaoverføringer = listOf() // TODO
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

        // TODO: Gjennomfør overføringen

        // TODO: Legg til behandlingen

        // TODO: Trenger egentlig for alle personer i gjeldende overføringer.
        val alleSaksnummerMapping = mapOf(
            behovet.fra to saksnummer.getValue(behovet.fra),
            behovet.til to saksnummer.getValue(behovet.til)
        )

        if (måVurderesManuelt) {
            packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
                OverføreKoronaOmsorgsdagerMelding.Løsningen()
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
                    HentPersonopplysningerMelding.behov(
                        HentPersonopplysningerMelding.BehovInput(
                            identitetsnummer = alleSaksnummerMapping.identitetsnummer()
                        )
                    )
                )
            )
        }

        return true
    }

}