package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.OverføreKoronaOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreKoronaOmsorgsdagerLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.ventPå

internal object KoronaoverføringerRapidVerktøy {

    internal fun TestRapid.gjennomførKoronaOverføring(
        barn: List<OverføreKoronaOmsorgsdagerBehov.Barn>,
        fra: Identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
        til: Identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
        fraSaksnummer: Saksnummer = "foo",
        tilSaksnummer: Saksnummer = "bar",
        omsorgsdagerTattUtIÅr: Int = 0,
        omsorgsdagerÅOverføre: Int = 10) : Pair<String, OverføreKoronaOmsorgsdagerLøsning> {

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            barn = barn
        )

        sendTestMessage(behovssekvens)
        ventPå(1)
        mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            fraSaksnummer = fraSaksnummer,
            tilSaksnummer = tilSaksnummer,
            relasjoner = barn.map {
                VurderRelasjonerMelding.Relasjon(identitetsnummer = it.identitetsnummer, relasjon = "BARN", borSammen = true)
            }.plus(VurderRelasjonerMelding.Relasjon(identitetsnummer = til, relasjon = "INGEN", borSammen = true)).toSet()
        )
        ventPå(2)

        mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        ventPå(3)


        val løsningsPar =  løsningOverføreKoronaOmsorgsdager()
        require(idStart == løsningsPar.first)
        return løsningsPar
    }


    internal fun TestRapid.opphørKoronaoverføringer() {

    }
}