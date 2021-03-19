package no.nav.omsorgspenger.overføringer.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.db.OpphørOverføringer
import no.nav.omsorgspenger.rivers.HentBehov
import no.nav.omsorgspenger.rivers.LeggTilLøsning
import java.time.LocalDate

internal object OpphøreOverføringerMelding :
    HentBehov<OpphøreOverføringerMelding.Behovet>,
    LeggTilLøsning<OpphørOverføringer.OpphørteOverføringer> {
    internal const val OpphøreOverføringer = "OpphøreOverføringer"

    internal data class Behovet(
        internal val fra: Saksnummer,
        internal val til: Saksnummer,
        internal val fraOgMed: LocalDate
    )

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.0.0")
        packet.interestedIn(
            BehovKeys.FraSaksnummer,
            BehovKeys.TilSaksnummer,
            BehovKeys.FraOgMed
        )
    }

    override fun hentBehov(packet: JsonMessage) = Behovet(
        fra = packet[BehovKeys.FraSaksnummer].asText(),
        til = packet[BehovKeys.TilSaksnummer].asText(),
        fraOgMed = packet[BehovKeys.FraOgMed].asLocalDate()
    )

    private object BehovKeys {
        val Versjon = "@behov.${OpphøreOverføringer}.versjon"
        val FraSaksnummer = "@behov.${OpphøreOverføringer}.fra.saksnummer"
        val TilSaksnummer = "@behov.${OpphøreOverføringer}.til.saksnummer"
        val FraOgMed = "@behov.${OpphøreOverføringer}.fraOgMed"
    }

    override fun løsning(løsning: OpphørOverføringer.OpphørteOverføringer): Pair<String, Map<String, *>> {
        return OpphøreOverføringer to mapOf(
            "deaktiverte" to løsning.deaktiverte,
            "nyTilOgMed" to løsning.nyTilOgMed
        )
    }
}