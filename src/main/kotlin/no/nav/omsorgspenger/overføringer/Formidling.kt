package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.formidling.Melding
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal object Formidling {
    internal fun opprettMeldingsBestillinger(
        behovssekvensId: BehovssekvensId,
        personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
        behandling: OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling) : List<Meldingsbestilling> {

        val saksnummer = behandling
            .gjeldendeOverføringer
            .entries
            .map { it.key to it.value.saksnummer }
            .toMap()

        if (!behandling.støtterAutomatiskMelding()) {
            return listOf()
        }

        val overføringerMedDager = behandling.overføringer.fjernOverføringerUtenDager()
        val bestillinger = mutableListOf<Meldingsbestilling>()

        saksnummer.forEach { (identitetsnummer, saksnummer) ->

            val melding = when (identitetsnummer) {
                overføreOmsorgsdager.overførerFra -> GittDager(
                    til = personopplysninger.getValue(overføreOmsorgsdager.overførerTil),
                    overføringer = behandling.overføringer,
                    antallDagerØnsketOverført = overføreOmsorgsdager.omsorgsdagerÅOverføre,
                    mottaksdato = overføreOmsorgsdager.mottaksdato
                )
                overføreOmsorgsdager.overførerTil -> MottattDager(
                    fra = personopplysninger.getValue(overføreOmsorgsdager.overførerFra),
                    overføringer = overføringerMedDager, // Mottaker får ingen 0-Overføringer.
                    mottaksdato = overføreOmsorgsdager.mottaksdato
                )
                else -> TidligerePartner(
                    mottaksdato = overføreOmsorgsdager.mottaksdato,
                    fraOgMed = overføringerMedDager.first().periode.fom
                )
            }

            bestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(identitetsnummer).aktørId,
                saksnummer = saksnummer,
                melding = melding,
                // Alle parter får svaret i brev om den som overfører dager
                // sendte inn per brev...
                måBesvaresPerBrev = behandling.måBesvaresPerBrev
            ))
        }
        return bestillinger
    }


    private fun OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling.støtterAutomatiskMelding() = when {
        ingenOverføringer -> meldingMåSendesManuelt("avslag")
        overføringer.size !in 1..2 -> meldingMåSendesManuelt("${overføringer.size} overføringer")
        else -> true
    }

    private fun meldingMåSendesManuelt(karakteristikk: String) =
        logger.warn("Melding(er) må sendes manuelt. Støtter ikke melding(er) for $karakteristikk. Se sikker logg for informasjon til melding(ene)").let { false }

    private val logger = LoggerFactory.getLogger(Formidling::class.java)
}

internal class GittDager(
    val til: Personopplysninger,
    val mottaksdato: LocalDate,
    val antallDagerØnsketOverført: Int,
    val overføringer: List<Overføring>
) : Melding {
    override val mal = "OVERFORE_GITT_DAGER"
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("antallDagerØnsketOverført", antallDagerØnsketOverført)
            root.put("overføringer", overføringer.somJSONArray())
            root.put("til", til.somJSONObject())
        }.toString()
    }()
}

internal class MottattDager(
    val fra: Personopplysninger,
    val mottaksdato: LocalDate,
    val overføringer: List<Overføring>
) : Melding {
    override val mal = "OVERFORE_MOTTATT_DAGER"
    override val data = {
        JSONObject().also { root ->
            root.put("mottaksdato", "$mottaksdato")
            root.put("overføringer", overføringer.somJSONArray())
            root.put("fra", fra.somJSONObject())
        }.toString()
    }()
}

internal class TidligerePartner(
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
}

private fun Personopplysninger.somJSONObject() : JSONObject? {
    return JSONObject().also { root ->
        navn?.also {
            root.put("navn", mapOf(
                "fornavn" to it.fornavn,
                "mellomnavn" to it.mellomnavn,
                "etternavn" to it.etternavn
            ))
        }
        root.put("fødselsdato", "$fødselsdato")
    }
}
private fun List<Overføring>.somJSONArray() = JSONArray().also {
    forEach { overføring ->
        it.put(mapOf(
            "gjelderFraOgMed" to "${overføring.periode.fom}",
            "gjelderTilOgMed" to "${overføring.periode.tom}",
            "antallDager" to overføring.antallDager,
            "starterGrunnet" to overføring.starterGrunnet.map { knekk -> knekk.dto() },
            "slutterGrunnet" to overføring.slutterGrunnet.map { knekk -> knekk.dto() }
        ))
    }
}
private fun Knekkpunkt.dto() = when(this) {
    // Vil alltid være i listen 'starterGrunnet' for første overføring
    Knekkpunkt.Mottaksdato -> "MOTTAKSDATO"
    Knekkpunkt.NullstillingAvForbrukteDager -> "NULLSTILLING_AV_FORBRUKTE_DAGER"
    Knekkpunkt.ForbrukteDagerIÅr -> "FORBRUKTE_DAGER_I_AAR"
    Knekkpunkt.OmsorgenForEtBarnStarter -> "OMSORGEN_FOR_ET_BARN_STARTER"
    Knekkpunkt.OmsorgenForEtBarnSlutter -> "OMSORGEN_FOR_ET_BARN_SLUTTER"
    Knekkpunkt.FordelingGirStarter -> "FORDELING_GIR_STARTER"
    Knekkpunkt.FordelingGirSlutter -> "FORDELING_GIR_SLUTTER"
    Knekkpunkt.MidlertidigAleneStarter -> "MIDLERTIDIG_ALENE_STARTER"
    Knekkpunkt.MidlertidigAleneSlutter -> "MIDLERTIDIG_ALENE_SLUTTER"
    // Vil alltid være en av disse to i 'sluttetGrunnet' for siste overføring
    Knekkpunkt.OmsorgenForSlutter -> "OMSORGEN_FOR_SLUTTER"
    Knekkpunkt.OmsorgenForMedUtvidetRettSlutter -> "OMSORGEN_FOR_MED_UTVIDET_RETT_SLUTTER"
}