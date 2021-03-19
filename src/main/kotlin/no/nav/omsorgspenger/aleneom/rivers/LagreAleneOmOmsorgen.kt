package no.nav.omsorgspenger.aleneom.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgen
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenService
import no.nav.omsorgspenger.aleneom.meldinger.AleneOmOmsorgenMelding
import no.nav.omsorgspenger.aleneom.meldinger.AleneOmOmsorgenPersonopplysningerMelding
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import org.slf4j.LoggerFactory

internal class LagreAleneOmOmsorgen(
    rapidsConnection: RapidsConnection,
    private val aleneOmOmsorgenService: AleneOmOmsorgenService,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "LagreAleneOmOmsorgen",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(LagreAleneOmOmsorgen::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(AleneOmOmsorgenMelding.AleneOmOmsorgen)
                it.harLøsningPåBehov(
                    HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
                    AleneOmOmsorgenPersonopplysningerMelding.HentPersonopplysninger
                )
                AleneOmOmsorgenMelding.validateBehov(it)
                HentOmsorgspengerSaksnummerMelding.validateLøsning(it)
                AleneOmOmsorgenPersonopplysningerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = AleneOmOmsorgenMelding.hentBehov(packet)
        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).getValue(behovet.identitetsnummer)
        val personopplysninger = AleneOmOmsorgenPersonopplysningerMelding.hentLøsning(packet).personopplysninger

        aleneOmOmsorgenService.lagreIForbindelseMedAleneOmOmsorgen(
            behovssekvensId = id,
            saksnummer = saksnummer,
            dato = behovet.mottaksdato,
            aleneOmOmsorgenFor = behovet.barn.map { AleneOmOmsorgen.Barn(
                identitetsnummer = it.identitetsnummer,
                fødselsdato = personopplysninger.getValue(it.identitetsnummer)
            )}
        )

        packet.leggTilLøsning(AleneOmOmsorgenMelding.AleneOmOmsorgen)

        return true
    }
}
