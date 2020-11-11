package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.formidling.Melding
import no.nav.omsorgspenger.overføringer.NyOverføring
import no.nav.omsorgspenger.overføringer.Personopplysninger
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal class GittDager(
    val til: Personopplysninger,
    val mottaksdato: LocalDate,
    val antallDagerØnsketOverført: Int,
    val formidlingsoverføringer : Formidlingsoverføringer
) : Melding {
    init { require(formidlingsoverføringer.støtterAutomatiskMelding) }
    override val mal = when  {
        formidlingsoverføringer.innvilget -> "OVERFORE_GITT_DAGER_INNVILGET"
        formidlingsoverføringer.avslått -> "OVERFORE_AVSLAG"
        else -> "OVERFORE_GITT_DAGER_DELVIS_INNVILGET"
    }
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("antallDagerØnsketOverført", antallDagerØnsketOverført)
            root.put("innvilgedeOverføringer", formidlingsoverføringer.innvilgedeOverføringer.somJSONArray(
                formidlingsoverføringer.startOgSluttGrunner
            ))
            root.put("avslåtteOverføringer", formidlingsoverføringer.avslåtteOverføringer.somJSONArray(
                formidlingsoverføringer.startOgSluttGrunner
            ))
            root.put("delvisInnvilgedeOverføringer", formidlingsoverføringer.delvisInnvilgedeOverføringer.somJSONArray(
                formidlingsoverføringer.startOgSluttGrunner
            ))
            root.put("til", til.somJSONObject())
        }.toString()
    }()
}

internal class MottattDager private constructor(
    val fra: Personopplysninger,
    val mottaksdato: LocalDate,
    val formidlingsoverføringer: Formidlingsoverføringer
) : Melding {
    init { require(formidlingsoverføringer.støtterAutomatiskMelding) }
    override val mal = when (formidlingsoverføringer.innvilget) {
        true -> "OVERFORE_MOTTATT_DAGER_INNVILGET"
        false -> "OVERFORE_MOTTATT_DAGER_DELVIS_INNVILGET"
    }
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("overføringer", formidlingsoverføringer.utenAvslåtteOverføringer.somJSONArray(
                formidlingsoverføringer.startOgSluttGrunner
            ))
            root.put("fra", fra.somJSONObject())
        }.toString()
    }()
    internal companion object {
        internal fun melding(
            fra: Personopplysninger,
            mottaksdato: LocalDate,
            formidlingsoverføringer: Formidlingsoverføringer) = when (formidlingsoverføringer.avslått) {
            true -> null
            else -> MottattDager(
                fra = fra,
                mottaksdato = mottaksdato,
                formidlingsoverføringer = formidlingsoverføringer
            )
        }
    }
}

internal class TidligerePartner private constructor(
    val fraOgMed: LocalDate,
    val mottaksdato: LocalDate
) : Melding {
    override val mal = "OVERFORE_TIDLIGERE_PARTNER"
    override val data = {
        @Language("JSON")
        val json = """
            {
              "fraOgMed": "$fraOgMed",
              "mottaksdato": "$mottaksdato"
            }
        """.trimIndent()
        json
    }()
    internal companion object {
        internal fun melding(
            mottaksdato: LocalDate,
            formidlingsoverføringer: Formidlingsoverføringer) = when(formidlingsoverføringer.avslått) {
            true -> null
            false -> TidligerePartner(
                mottaksdato = mottaksdato,
                fraOgMed = formidlingsoverføringer.utenAvslåtteOverføringer.first().periode.fom
            )
        }
    }
}


private fun Personopplysninger.somJSONObject() = JSONObject().also { root ->
    root.put("navn", mapOf(
        "fornavn" to navn!!.fornavn,
        "mellomnavn" to navn.mellomnavn,
        "etternavn" to navn.etternavn
    ))
    root.put("fødselsdato", "$fødselsdato")
}

private fun List<NyOverføring>.somJSONArray(
    startOgSluttGrunner: Map<NyOverføring, Pair<StartGrunn, SluttGrunn>>
) = JSONArray().also {
    forEach { overføring ->
        val startOgSluttGrunn = startOgSluttGrunner.getValue(overføring)
        it.put(mapOf(
            "gjelderFraOgMed" to "${overføring.periode.fom}",
            "gjelderTilOgMed" to "${overføring.periode.tom}",
            "antallDager" to overføring.antallDager,
            "starterGrunnet" to startOgSluttGrunn.first.name,
            "slutterGrunnet" to startOgSluttGrunn.second.name
        ))
    }
}