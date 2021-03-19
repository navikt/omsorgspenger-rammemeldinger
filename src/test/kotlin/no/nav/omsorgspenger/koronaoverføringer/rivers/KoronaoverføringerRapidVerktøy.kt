package no.nav.omsorgspenger.koronaoverføringer.rivers

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreKoronaOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreKoronaOmsorgsdagerLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.printSisteMelding
import no.nav.omsorgspenger.testutils.sisteMeldingSomJSONObject
import no.nav.omsorgspenger.testutils.ventPå
import org.junit.jupiter.api.Assertions.assertNotNull
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

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


    internal fun TestRapid.opphørKoronaoverføringer(
        fra: Saksnummer, til: Saksnummer, fraOgMed: LocalDate) {
        val (_, behovssekvens) = Behovssekvens(
            id = ULID().nextULID(),
            correlationId = "${UUID.randomUUID()}",
            behov = arrayOf(Behov(
                navn = "OpphøreKoronaOverføringer",
                input = mapOf(
                    "versjon" to "1.0.0",
                    "fraOgMed" to "$fraOgMed",
                    "fra" to mapOf(
                        "saksnummer" to fra
                    ),
                    "til" to mapOf(
                        "saksnummer" to til
                    )
                )
            ))
        ).keyValue

        sendTestMessage(behovssekvens)
        printSisteMelding()
        ventPå(1)
        val løst = sisteMeldingSomJSONObject().getJSONObject("@løsninger").getJSONObject("OpphøreKoronaOverføringer").getString("løst").let {
            ZonedDateTime.parse(it)
        }
        assertNotNull(løst)
    }
}