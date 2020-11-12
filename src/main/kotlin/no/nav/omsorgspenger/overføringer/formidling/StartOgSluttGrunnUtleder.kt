package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.overføringer.Knekkpunkt
import no.nav.omsorgspenger.overføringer.NyOverføring

internal object StartOgSluttGrunnUtleder {
    internal fun startOgSluttGrunn(
        innvilget: Boolean,
        avslått: Boolean,
        alleOverføringer: List<NyOverføring>,
        innvilgedeOverføringer: List<NyOverføring>,
        delvisInnvilgedeOverføringer: List<NyOverføring>,
        avslåtteOverføringer: List<NyOverføring>,
    ) : Pair<Grunn, Grunn>? = when {
        alleOverføringer.map { it.starterGrunnet.plus(it.slutterGrunnet) }.flatten().inneholder(
            Knekkpunkt.MidlertidigAleneStarter, Knekkpunkt.MidlertidigAleneSlutter
        ) -> null
        innvilget -> innvilgedeOverføringer.first().innvilget()
        avslått -> avslåtteOverføringer.first().avslag()
        else -> null
    }
}

private fun NyOverføring.innvilget() = when {
    slutterGrunnet.inneholder(Knekkpunkt.OmsorgenForMedUtvidetRettSlutter) ->
        Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
    slutterGrunnet.inneholder(Knekkpunkt.OmsorgenForSlutter) ->
        Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
    else -> null
}

private fun NyOverføring.avslag() = when {
    starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter) ->
        Grunn.PÅGÅENDE_FORDELING to Grunn.PÅGÅENDE_FORDELING
    else -> null
}

internal enum class Grunn {
    MOTTAKSDATO,
    PÅGÅENDE_FORDELING,
    OMSORGEN_FOR_BARN_OPPHØRER,
    OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
}

private fun List<Knekkpunkt>.inneholder(vararg others: Knekkpunkt) =
    containsAll(others.toList())