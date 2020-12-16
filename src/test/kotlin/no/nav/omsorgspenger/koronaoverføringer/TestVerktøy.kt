package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import java.time.LocalDate
import java.time.ZonedDateTime

internal object TestVerktøy {
    internal fun grunnlag(
        behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
        overføringer: List<SpleisetOverføringGitt> = emptyList(),
        fordelinger: List<FordelingGirMelding> = emptyList(),
        koronaoverføringer: List<GjeldendeOverføringGitt> = emptyList(),
        utvidetRett: List<UtvidetRettVedtak> = emptyList()
    ) = Grunnlag(
        behovet = behovet,
        overføringer = overføringer,
        fordelinger = fordelinger,
        koronaoverføringer = koronaoverføringer,
        utvidetRett = utvidetRett)

    internal fun behovet(
        omsorgsdagerTattUtIÅr: Int = 10,
        barn: List<OverføreKoronaOmsorgsdagerMelding.Barn> = emptyList()
    ) = OverføreKoronaOmsorgsdagerMelding.Behovet(
        fra = IdentitetsnummerGenerator.identitetsnummer(),
        til = IdentitetsnummerGenerator.identitetsnummer(),
        jobberINorge = true,
        periode = Periode("2021-01-01/2021-12-31"),
        mottatt = ZonedDateTime.now(),
        mottaksdato = LocalDate.now(),
        journalpostIder = setOf("123"),
        omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
        omsorgsdagerÅOverføre = 10,
        barn = barn
    )

    internal fun barn(
        fødselsdato: LocalDate = LocalDate.now().minusYears(5),
        aleneOmOmsorgen: Boolean = false,
        utvidetRett: Boolean = false
    ) = OverføreKoronaOmsorgsdagerMelding.Barn(
        identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
        fødselsdato = fødselsdato,
        aleneOmOmsorgen = aleneOmOmsorgen,
        utvidetRett = utvidetRett
    )
}