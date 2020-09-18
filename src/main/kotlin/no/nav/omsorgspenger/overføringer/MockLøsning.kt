package no.nav.omsorgspenger.overføringer

import java.time.LocalDate

internal object MockLøsning {
    internal fun mockLøsning(
        utfall: String,
        begrunnelser: List<String>,
        fra: String,
        til: String,
        omsorgsdagerÅOverføre: Int
    ): Map<String, Any?> {
        val overføringer = mapOf(
            fra to mapOf(
                "gitt" to listOf(
                    mapOf(
                        "antallDager" to omsorgsdagerÅOverføre,
                        "gjelderFraOgMed" to LocalDate.now(),
                        "gjelderTilOgMed" to LocalDate.now().plusYears(1),
                        "til" to mapOf(
                            "navn" to "Kari Nordmann",
                            "fødselsdato" to LocalDate.now().minusYears(30)
                        )
                    )
                ),
                "fått" to emptyList()
            ),
            til to mapOf(
                "fått" to listOf(
                    mapOf(
                        "antallDager" to omsorgsdagerÅOverføre,
                        "gjelderFraOgMed" to LocalDate.now(),
                        "gjelderTilOgMed" to LocalDate.now().plusYears(1),
                        "fra" to mapOf(
                            "navn" to "Ola Nordmann",
                            "fødselsdato" to LocalDate.now().minusYears(35)
                        )
                    )
                ),
                "gitt" to emptyList()
            )
        )

        return mapOf(
            "utfall" to utfall,
            "begrunnelser" to begrunnelser,
            "overføringer" to when (utfall) {
                "Gjennomført", "Avslått" -> overføringer
                else -> emptyMap()
            }
        )
    }
}