package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.overføringer.Knekkpunkt
import no.nav.omsorgspenger.overføringer.NyOverføring

internal object StartOgSluttGrunnUtleder {
    internal fun NyOverføring.startOgSluttGrunn(
        første: Boolean,
    ) : Pair<StartGrunn, SluttGrunn>? {
        if (inneholderGrunnerSomIkkeStøttes()) return null

        val start = when {
            starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) ->
                StartGrunn.PÅGÅENDE_FORDELING_OG_FORBRUKTE_DAGER_I_ÅR
            starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter) ->
                StartGrunn.PÅGÅENDE_FORDELING
            starterGrunnet.inneholder(Knekkpunkt.ForbrukteDagerIÅr) ->
                StartGrunn.FORBRUKTE_DAGER_I_ÅR
            starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                StartGrunn.NULLSTILLING_AV_FORBRUKTE_DAGER
            første ->
                StartGrunn.MOTTAKSDATO
            else -> null
        }
        val slutt = when {
            slutterGrunnet.inneholder(Knekkpunkt.FordelingGirSlutter, Knekkpunkt.NullstillingAvForbrukteDager) ->
                SluttGrunn.AVSLUTTET_FORDELING_OG_NULLSILLING_AV_FORBRUKTE_DAGER
            slutterGrunnet.inneholder(Knekkpunkt.FordelingGirSlutter) ->
                SluttGrunn.AVSLUTTET_FORDELING
            slutterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                SluttGrunn.NULLSTILLING_AV_FORBRUKTE_DAGER
            slutterGrunnet.inneholder(Knekkpunkt.OmsorgenForSlutter) ->
                SluttGrunn.OMSORGEN_FOR_BARN_OPPHØRER
            slutterGrunnet.inneholder(Knekkpunkt.OmsorgenForMedUtvidetRettSlutter) ->
                SluttGrunn.OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
            else -> null
        }

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun NyOverføring.inneholderGrunnerSomIkkeStøttes() =
        starterGrunnet.plus(slutterGrunnet).any {
            it == Knekkpunkt.MidlertidigAleneStarter ||
            it == Knekkpunkt.MidlertidigAleneSlutter
        }
}

internal enum class StartGrunn {
    MOTTAKSDATO,
    PÅGÅENDE_FORDELING,
    FORBRUKTE_DAGER_I_ÅR,
    NULLSTILLING_AV_FORBRUKTE_DAGER,
    PÅGÅENDE_FORDELING_OG_FORBRUKTE_DAGER_I_ÅR
}

internal enum class SluttGrunn {
    AVSLUTTET_FORDELING,
    NULLSTILLING_AV_FORBRUKTE_DAGER,
    AVSLUTTET_FORDELING_OG_NULLSILLING_AV_FORBRUKTE_DAGER,
    OMSORGEN_FOR_BARN_OPPHØRER,
    OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
}

private fun List<Knekkpunkt>.inneholder(vararg others: Knekkpunkt) =
    containsAll(others.toList())