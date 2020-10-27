package no.nav.omsorgspenger.formidling

import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Saksnummer
import org.intellij.lang.annotations.Language
import org.json.JSONObject

internal class Meldingsbestilling(
    val behovssekvensId: BehovssekvensId,
    val aktørId: AktørId,
    val saksnummer: Saksnummer,
    val melding: Melding,
    val måBesvaresPerBrev: Boolean) {
    internal val keyValue: Pair<String, String> = {
        @Language("JSON")
        val bestilling =
            """
            {
                "eksternReferanse": "$behovssekvensId",
                "dokumentbestillingId": "$behovssekvensId-$aktørId",
                "aktørId": "$aktørId",
                "ytelseType": "OMSORGSPENGER",
                "saksnummer": "$saksnummer",
                "dokumentMal": "${melding.mal}",
                "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
                "distribuere" : $måBesvaresPerBrev
            }
            """.trimEnd()

        "$behovssekvensId-$aktørId" to JSONObject(bestilling).also {
            it.put("dokumentdata", JSONObject(melding.data))
        }.toString()
    }()
}

internal interface Melding {
    val mal: String
    val data: String
}