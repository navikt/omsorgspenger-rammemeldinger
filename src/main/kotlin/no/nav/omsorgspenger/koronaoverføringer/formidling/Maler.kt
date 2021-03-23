package no.nav.omsorgspenger.koronaoverføringer.formidling

import no.nav.omsorgspenger.formidling.Melding
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerPersonopplysningerMelding
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal class GittDager(
    val til: OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger,
    val mottaksdato: LocalDate,
    val antallDagerØnsketOverført: Int,
    val overføring: NyOverføring) : Melding {
    override val mal = "KORONA_OVERFORE_GITT_DAGER"
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("antallDagerØnsketOverført", antallDagerØnsketOverført)
            root.put("overføringer", mapOf(
                "gjelderFraOgMed" to "${overføring.periode.fom}",
                "gjelderTilOgMed" to "${overføring.periode.tom}",
                "antallDager" to overføring.antallDager
            ).let { JSONArray().put(it)})
            root.put("til", til.somJSONObject())
        }.toString()
    }()
}

internal class GittDagerOpphørt(
    val til: OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger,
    val fraOgMed: LocalDate
) : Melding {
    override val mal = "KORONA_OVERFORE_GITT_DAGER_OPPHORT"
    override val data = {
        JSONObject().also { root ->
            root.put("fraOgMed", "$fraOgMed")
            root.put("til", til.somJSONObject())
        }.toString()
    }()
}

internal class MottattDager(
    val fra: OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger,
    val mottaksdato: LocalDate,
    val overføring: NyOverføring) : Melding {
    override val mal = "KORONA_OVERFORE_MOTTATT_DAGER"
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("overføringer", mapOf(
                "gjelderFraOgMed" to "${overføring.periode.fom}",
                "gjelderTilOgMed" to "${overføring.periode.tom}",
                "antallDager" to overføring.antallDager
            ).let { JSONArray().put(it)})
            root.put("fra", fra.somJSONObject())
        }.toString()
    }()
}

internal class MottattDagerOpphørt(
    val fra: OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger,
    val fraOgMed: LocalDate
) : Melding {
    override val mal = "KORONA_OVERFORE_MOTTATT_DAGER_OPPHORT"
    override val data = {
        JSONObject().also { root ->
            root.put("fraOgMed", "$fraOgMed")
            root.put("fra", fra.somJSONObject())
        }.toString()
    }()
}

internal class Avslag(
    val mottaksdato: LocalDate,
    val antallDagerØnsketOverført: Int,
    val til: OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger
) : Melding {
    override val mal = "KORONA_OVERFORE_AVSLAG"
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("antallDagerØnsketOverført", antallDagerØnsketOverført)
            root.put("til", til.somJSONObject())
        }.toString()
    }()
}

private fun OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger.somJSONObject() = JSONObject().also { root ->
    root.put("navn", mapOf(
        "fornavn" to navn!!.fornavn,
        "mellomnavn" to navn.mellomnavn,
        "etternavn" to navn.etternavn
    ))
    root.put("fødselsdato", "$fødselsdato")
}