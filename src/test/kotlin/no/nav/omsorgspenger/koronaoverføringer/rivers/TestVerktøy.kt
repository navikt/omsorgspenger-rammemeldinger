package no.nav.omsorgspenger.koronaoverføringer.rivers

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreKoronaOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreKoronaOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.sisteMelding
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

internal fun behovssekvensOverføreKoronaOmsorgsdager(
    id: String = ULID().nextValue().toString(),
    fra: Identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
    til: Identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
    jobberINorge: Boolean = true,
    omsorgsdagerTattUtIÅr: Int = 0,
    omsorgsdagerÅOverføre: Int,
    periode: Periode = Periode("2021-01-01/2021-12-31"),
    mottatt: ZonedDateTime = ZonedDateTime.now(),
    barn: List<OverføreKoronaOmsorgsdagerBehov.Barn> = listOf(koronaBarn()),
    journalpostIder: List<String> = listOf("1234")
) = Behovssekvens(
    id = id,
    correlationId = "${UUID.randomUUID()}",
    behov = arrayOf(OverføreKoronaOmsorgsdagerBehov(
        fra = OverføreKoronaOmsorgsdagerBehov.OverførerFra(identitetsnummer = fra, jobberINorge = jobberINorge),
        til = OverføreKoronaOmsorgsdagerBehov.OverførerTil(identitetsnummer = til),
        omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
        omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
        journalpostIder = journalpostIder,
        periode = OverføreKoronaOmsorgsdagerBehov.Periode(
            fraOgMed = periode.fom,
            tilOgMed = periode.tom
        ),
        mottatt = mottatt,
        barn = barn
    ))
).keyValue

internal fun koronaBarn(utvidetRett: Boolean = false) = OverføreKoronaOmsorgsdagerBehov.Barn(
    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
    fødselsdato = LocalDate.now().minusYears(1),
    aleneOmOmsorgen = false,
    utvidetRett = utvidetRett
)

internal fun JSONObject.assertGosysJournalføringsoppgave(
    behovssekvensId: BehovssekvensId,
    fra: Identitetsnummer,
    til: Identitetsnummer,
    journalpostId: JournalpostId,
    forventetLøsninger: List<String> = listOf("OverføreKoronaOmsorgsdager")
) {
    assertEquals(getString(Behovsformat.Id), behovssekvensId)
    assertThat(forventetLøsninger.plus("OpprettGosysJournalføringsoppgaver")).hasSameElementsAs(
        getJSONArray(Behovsformat.Behovsrekkefølge).map { it.toString() }
    )
    @Language("JSON")
    val forventetBehovInput = """
            {
             "identitetsnummer": "$fra",
             "berørteIdentitetsnummer": ["$til"],
             "journalpostType": "OverføreKoronaOmsorgsdager",
             "journalpostIder": ["$journalpostId"]
            }
            """.trimIndent()
    val faktiskBehovInput = getJSONObject("@behov").getJSONObject("OpprettGosysJournalføringsoppgaver").toString()
    JSONAssert.assertEquals(forventetBehovInput, faktiskBehovInput, true)
    assertThat(forventetLøsninger).hasSameElementsAs(
        getJSONObject("@løsninger").keySet()
    )
}

internal fun TestRapid.løsningOverføreKoronaOmsorgsdager() =
    sisteMelding().somMelding().løsningPå(OverføreKoronaOmsorgsdagerLøsningResolver.Instance)
