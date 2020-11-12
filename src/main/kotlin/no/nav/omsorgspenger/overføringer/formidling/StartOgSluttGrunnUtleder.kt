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
        alleOverføringer.size == 1 && delvisInnvilgedeOverføringer.size == 1 -> enDelvis(
            delvisInnvilget = delvisInnvilgedeOverføringer.first()
        )
        avslåtteOverføringer.size == 1 && innvilgedeOverføringer.size == 1 -> bruktAlleDagerIÅr(
            avslått = avslåtteOverføringer.first(),
            innvilget = innvilgedeOverføringer.first()
        )
        delvisInnvilgedeOverføringer.size == 1 && innvilgedeOverføringer.size == 1 -> bruktNoenDagerIÅr(
            delvisInnvilget = delvisInnvilgedeOverføringer.first(),
            innvilget = innvilgedeOverføringer.first()
        )
        avslåtteOverføringer.size == 1 && delvisInnvilgedeOverføringer.size == 1 -> bruktAlleDagerIÅrOgFordeler(
            avslått = avslåtteOverføringer.first(),
            delvisInnvilget = delvisInnvilgedeOverføringer.first()
        )
        delvisInnvilgedeOverføringer.size == 2 -> toDelvis(
            delvisEn = delvisInnvilgedeOverføringer.first(),
            delvisTo = delvisInnvilgedeOverføringer[1]
        )
        else -> null
    }

    private fun toDelvis(delvisEn: NyOverføring, delvisTo: NyOverføring): Pair<Grunn, Grunn>? {
        val start = when {
            delvisEn.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisTo.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.PÅGÅENDE_FORDELING
            else -> null
        }
        val slutt = delvisTo.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun bruktAlleDagerIÅrOgFordeler(avslått: NyOverføring, delvisInnvilget: NyOverføring): Pair<Grunn, Grunn>? {
        val start = when {
            avslått.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_ALLE_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING
            else -> null
        }
        val slutt = delvisInnvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun enDelvis(delvisInnvilget: NyOverføring): Pair<Grunn, Grunn>? {
        val start = when {
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter) &&
            delvisInnvilget.starterGrunnet.inneholderIkke(Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.slutterGrunnet.inneholder(Knekkpunkt.FordelingGirSlutter) ->
                Grunn.PÅGÅENDE_FORDELING
            else -> null
        }
        val slutt = delvisInnvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun bruktNoenDagerIÅr(
        delvisInnvilget: NyOverføring,
        innvilget: NyOverføring
    ): Pair<Grunn, Grunn>? {
        val start = when {
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.slutterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_NOEN_DAGER_I_ÅR
            else -> null
        }

        val slutt = innvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun bruktAlleDagerIÅr(avslått: NyOverføring, innvilget: NyOverføring) : Pair<Grunn, Grunn>? {
        val start = when {
            avslått.slutterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_ALLE_DAGER_I_ÅR
            else -> null
        }
        val slutt = innvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
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
    BRUKT_ALLE_DAGER_I_ÅR,
    BRUKT_NOEN_DAGER_I_ÅR,
    BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING,
    BRUKT_ALLE_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING,
    PÅGÅENDE_FORDELING,
    OMSORGEN_FOR_BARN_OPPHØRER,
    OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
}

private fun List<Knekkpunkt>.inneholder(vararg others: Knekkpunkt) =
    containsAll(others.toList())

private fun List<Knekkpunkt>.inneholderIkke(vararg others: Knekkpunkt) =
    !inneholder(*others)