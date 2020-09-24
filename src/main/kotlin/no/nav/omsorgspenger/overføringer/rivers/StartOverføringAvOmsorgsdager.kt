package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.somLøsning
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.Vurderinger
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.somLøsning

import org.slf4j.LoggerFactory

internal class StartOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.utenLøsningPåBehov(OverføreOmsorgsdagerBehandlingMelding.Navn)
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("$id -> StartOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold()

        logger.info("hentFordelingGirMeldinger")
        val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        logger.info("hentUtvidetRettVedtak")
        val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val behandling = Behandling()
        val grunnlag = Vurderinger.vurderOmsorgenFor(
            grunnlag = Grunnlag(
                overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold(),
                utvidetRettVedtak = utvidetRettVedtak,
                fordelingGirMeldinger = fordelingGirMeldinger
            ),
            behandling = behandling
        )

        logger.info("vurderInngangsvilkår")
        Vurderinger.vurderInngangsvilkår(
            grunnlag = grunnlag,
            behandling = behandling
        )

        logger.info("beregnOmsorgsdagerTilgjengeligForOverføring")
        val omsorgsdagerTilgjengeligForOverføring = Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring(
            grunnlag = grunnlag,
            behandling = behandling
        )

        logger.info("genererer overføringer")
        val overføringer = omsorgsdagerTilgjengeligForOverføring.somOverføringer()


        logger.info("karakteristikker = ${behandling.karakteristikker()}")

        val inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre = omsorgsdagerTilgjengeligForOverføring.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(
            ønsketOmsorgsdagerÅOverføre = grunnlag.overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        if (behandling.oppfyllerIkkeInngangsvilkår()) {
            logger.info("** Avslå **")
        }

        if (inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre && behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett()) {
            logger.info("** Sende til opprettelse av Gosys-oppgave **")
        }

        logger.info("legger til behov med løsninger [${HentFordelingGirMeldingerMelding.Navn}, ${HentUtvidetRettVedtakMelding.Navn}, ${OverføreOmsorgsdagerBehandlingMelding.Navn}]")
        logger.warn("Løsning på behovet ${HentUtvidetRettVedtakMelding.Navn} bør flyttes til 'omsorgspenger-rammevedtak'")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
            behovMedLøsninger = arrayOf(
                Behov(
                    navn = HentFordelingGirMeldingerMelding.Navn,
                    input = HentFordelingGirMeldingerMelding.input(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        periode = overføreOmsorgsdager.overordnetPeriode
                    )
                ) to fordelingGirMeldinger.somLøsning(),
                Behov(
                    navn = HentUtvidetRettVedtakMelding.Navn,
                    input = HentUtvidetRettVedtakMelding.input(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        periode = overføreOmsorgsdager.overordnetPeriode
                    )
                ) to utvidetRettVedtak.somLøsning(),
                Behov(
                    navn = OverføreOmsorgsdagerBehandlingMelding.Navn,
                ) to behandling.somLøsning(
                    nyeOverføringer = overføringer
                )
            )
        )

        logger.info("legger til behov [${HentPersonopplysningerMelding.Navn}]")
        packet.leggTilBehov(
            aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
            behov = arrayOf(Behov(
                navn = HentPersonopplysningerMelding.Navn,
                input = HentPersonopplysningerMelding.input(
                    identitetsnummer = setOf(overføreOmsorgsdager.overførerFra, overføreOmsorgsdager.overførerTil)
                )
            ))
        )

        println(packet.toJson())

        context.sendMedId(packet)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(StartOverføringAvOmsorgsdager::class.java)
    }
}