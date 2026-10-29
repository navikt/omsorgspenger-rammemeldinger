package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode

internal data class KnektPeriode(
    internal val periode: Periode,
    internal val starterGrunnet: List<Knekkpunkt>,
    internal val slutterGrunnet: List<Knekkpunkt>
)

internal enum class Knekkpunkt {
    Mottaksdato,
    ForbrukteDagerIÅr,
    NullstillingAvForbrukteDager,
    OmsorgenForEtBarnStarter,
    OmsorgenForEtBarnSlutter,
    FordelingGirStarter,
    FordelingGirSlutter,
    KoronaOverføringGirStarter,
    KoronaOverføringGirSlutter,
    MidlertidigAleneStarter,
    MidlertidigAleneSlutter,
    OmsorgenForSlutter,
    OmsorgenForMedUtvidetRettSlutter
}
