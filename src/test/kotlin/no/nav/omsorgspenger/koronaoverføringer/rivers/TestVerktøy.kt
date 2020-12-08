package no.nav.omsorgspenger.koronaoverføringer.rivers

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreKoronaOmsorgsdagerBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.overføringer.IdentitetsnummerGenerator
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
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
    barn: List<OverføreKoronaOmsorgsdagerBehov.Barn> = listOf(barn()),
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

internal fun JsonMessage.leggTilLøsningPåHenteOmsorgspengerSaksnummer(
    fra: Identitetsnummer, til: Identitetsnummer) = leggTilLøsning(
    behov = HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
    løsning = mapOf(
        "saksnummer" to mapOf(
            fra to "foo",
            til to "bar"
        )
    )
)

internal fun barn(utvidetRett: Boolean = false) = OverføreKoronaOmsorgsdagerBehov.Barn(
    identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
    fødselsdato = LocalDate.now().minusYears(1),
    aleneOmOmsorgen = false,
    utvidetRett = false
)

internal fun JSONObject.assertGosysJournalføringsoppgave(
    behovssekvensId: BehovssekvensId,
    fra: Identitetsnummer,
    til: Identitetsnummer,
    journalpostId: JournalpostId
) {
    assertEquals(getString(Behovsformat.Id), behovssekvensId)
    assertEquals(
        expected = listOf("OverføreKoronaOmsorgsdager", "OpprettGosysJournalføringsoppgaver"),
        actual = getJSONArray(Behovsformat.Behovsrekkefølge).map { it.toString() }
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
    assertEquals(
        expected = setOf("OverføreKoronaOmsorgsdager"),
        actual = getJSONObject("@løsninger").keySet()
    )
}