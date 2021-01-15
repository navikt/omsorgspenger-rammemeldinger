package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.overføringer.Behandling
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
        karakteristikker: Set<Behandling.Karakteristikk>
    ) : Pair<Grunn, Grunn>? = when {
        alleOverføringer.map { it.starterGrunnet.plus(it.slutterGrunnet) }.flatten().inneholder(
            Knekkpunkt.MidlertidigAleneStarter, Knekkpunkt.MidlertidigAleneSlutter
        ) -> null
        innvilget -> innvilgedeOverføringer.first().innvilget()
        avslått -> avslag(
            karakteristikker = karakteristikker,
            overføring = avslåtteOverføringer.firstOrNull()
        )
        alleOverføringer.size == 1 && delvisInnvilgedeOverføringer.size == 1 -> kunEnDelvis(
            delvisInnvilget = delvisInnvilgedeOverføringer.first()
        )
        avslåtteOverføringer.size == 1 && innvilgedeOverføringer.size == 1 -> enAvslåttEnInnvilget(
            avslått = avslåtteOverføringer.first(),
            innvilget = innvilgedeOverføringer.first()
        )
        delvisInnvilgedeOverføringer.size == 1 && innvilgedeOverføringer.size == 1 -> enDelvisEnInnvilget(
            delvisInnvilget = delvisInnvilgedeOverføringer.first(),
            innvilget = innvilgedeOverføringer.first()
        )
        avslåtteOverføringer.size == 1 && delvisInnvilgedeOverføringer.size == 1 -> enAvslåttEnDelvis(
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
            delvisEn.starterGrunnet.inneholder(Knekkpunkt.KoronaOverføringGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
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

    private fun enAvslåttEnDelvis(avslått: NyOverføring, delvisInnvilget: NyOverføring): Pair<Grunn, Grunn>? {
        val start = when {
            avslått.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_ALLE_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING
            avslått.starterGrunnet.inneholder(Knekkpunkt.KoronaOverføringGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
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

    private fun kunEnDelvis(delvisInnvilget: NyOverføring): Pair<Grunn, Grunn>? {
        val start = when {
            delvisInnvilget.starterGrunnet.inneholderMinstEn(Knekkpunkt.FordelingGirStarter, Knekkpunkt.KoronaOverføringGirStarter) &&
            delvisInnvilget.starterGrunnet.inneholderIkke(Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.slutterGrunnet.inneholderMinstEn(Knekkpunkt.FordelingGirSlutter, Knekkpunkt.KoronaOverføringGirSlutter) ->
                Grunn.PÅGÅENDE_FORDELING
            else -> null
        }
        val slutt = delvisInnvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun enDelvisEnInnvilget(
        delvisInnvilget: NyOverføring,
        innvilget: NyOverføring
    ): Pair<Grunn, Grunn>? {
        val start = when {
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.FordelingGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.KoronaOverføringGirStarter, Knekkpunkt.ForbrukteDagerIÅr) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING
            delvisInnvilget.starterGrunnet.inneholder(Knekkpunkt.ForbrukteDagerIÅr) &&
            delvisInnvilget.slutterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_NOEN_DAGER_I_ÅR
            innvilget.starterGrunnet.inneholderMinstEn(Knekkpunkt.FordelingGirSlutter, Knekkpunkt.KoronaOverføringGirSlutter) &&
            innvilget.starterGrunnet.inneholderIkke(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.PÅGÅENDE_FORDELING
            else -> null
        }

        val slutt = innvilget.innvilget()?.second

        return when {
            start != null && slutt != null -> start to slutt
            else -> null
        }
    }

    private fun enAvslåttEnInnvilget(avslått: NyOverføring, innvilget: NyOverføring) : Pair<Grunn, Grunn>? {
        val start = when {
            avslått.slutterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) &&
            innvilget.starterGrunnet.inneholder(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.BRUKT_ALLE_DAGER_I_ÅR
            innvilget.starterGrunnet.inneholderMinstEn(Knekkpunkt.FordelingGirSlutter, Knekkpunkt.KoronaOverføringGirSlutter) &&
            innvilget.starterGrunnet.inneholderIkke(Knekkpunkt.NullstillingAvForbrukteDager) ->
                Grunn.PÅGÅENDE_FORDELING
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

private fun avslag(
    overføring: NyOverføring?,
    karakteristikker: Set<Behandling.Karakteristikk>) = when {
    karakteristikker.contains(Behandling.Karakteristikk.IkkeSammeAdresseSomMottaker) ->
        Grunn.IKKE_SAMME_ADRESSE_SOM_MOTTAKER to Grunn.IKKE_SAMME_ADRESSE_SOM_MOTTAKER
    karakteristikker.contains(Behandling.Karakteristikk.IkkeOmsorgenForNoenBarn) ->
        Grunn.IKKE_OMSORGEN_FOR_NOEN_BARN to Grunn.IKKE_OMSORGEN_FOR_NOEN_BARN
    overføring != null && overføring.starterGrunnet.inneholderMinstEn(Knekkpunkt.FordelingGirStarter, Knekkpunkt.KoronaOverføringGirStarter) ->
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
    OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER,
    IKKE_OMSORGEN_FOR_NOEN_BARN,
    IKKE_SAMME_ADRESSE_SOM_MOTTAKER
}

private fun List<Knekkpunkt>.inneholder(vararg others: Knekkpunkt) =
    containsAll(others.toList())

private fun List<Knekkpunkt>.inneholderMinstEn(vararg others: Knekkpunkt) = intersect(others.toList()).isNotEmpty()

private fun List<Knekkpunkt>.inneholderIkke(vararg others: Knekkpunkt) =
    !inneholder(*others)