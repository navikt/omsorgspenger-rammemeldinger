package no.nav.omsorgspenger.overføringer


internal object MockLøsning {
    internal fun mockLøsning(
        utfall: Utfall,
        begrunnelser: List<String>,
        fra: String,
        til: String,
        overføringer: List<Overføring>,
        parter: Set<Part>
    ): Map<String, Any?> {
        val gitt = (parter.firstOrNull { it.identitetsnummer == til }?.let { overføringerTil(overføringer, it) })?: listOf()
        val fått = (parter.firstOrNull { it.identitetsnummer == fra }?.let { overføringerFra(overføringer, it) })?: listOf()
        val mappedOverføringer = mapOf<String, Any?>(
            fra to mapOf(
                "gitt" to gitt,
                "fått" to emptyList()
            ),
            til to mapOf(
                "fått" to fått,
                "gitt" to emptyList()
            )
        )

        return mapOf(
            "utfall" to utfall.name,
            "begrunnelser" to begrunnelser,
            "overføringer" to when (utfall) {
                Utfall.Gjennomført, Utfall.Avslått -> mappedOverføringer
                else -> emptyMap()
            }
        )
    }



    private fun overføringerTil(overføringer: List<Overføring>, til: Part) = overføringer.map { mapOf(
        "antallDager" to it.antallDager,
        "gjelderFraOgMed" to it.periode.fom,
        "gjelderTilOgMed" to it.periode.tom,
        "til" to mapOf(
            "navn" to til.navn,
            "fødselsdato" to til.fødselsdato
        )
    )}

    private fun overføringerFra(overføringer: List<Overføring>, fra: Part) = overføringer.map { mapOf(
        "antallDager" to it.antallDager,
        "gjelderFraOgMed" to it.periode.fom,
        "gjelderTilOgMed" to it.periode.tom,
        "fra" to mapOf(
            "navn" to fra.navn,
            "fødselsdato" to fra.fødselsdato
        )
    )}
}