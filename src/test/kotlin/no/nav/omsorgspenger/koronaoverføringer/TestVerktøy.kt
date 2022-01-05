package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal object TestVerktøy {
    internal fun grunnlag(
        behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
        overføringer: List<SpleisetOverføringGitt> = emptyList(),
        fordelinger: List<FordelingGirMelding> = emptyList(),
        koronaoverføringer: List<SpleisetOverføringGitt> = emptyList(),
        utvidetRett: List<UtvidetRettVedtak> = emptyList(),
        relasjoner: Set<VurderRelasjonerMelding.Relasjon> = emptySet()
    ) = Grunnlag(
        behovet = behovet,
        overføringer = overføringer,
        fordelinger = fordelinger,
        koronaoverføringer = koronaoverføringer,
        utvidetRett = utvidetRett,
        relasjoner = relasjoner)

    internal fun behovet(
        omsorgsdagerTattUtIÅr: Int = 10,
        barn: List<OverføreKoronaOmsorgsdagerMelding.Barn> = emptyList()
    ) = OverføreKoronaOmsorgsdagerMelding.Behovet(
        fra = IdentitetsnummerGenerator.identitetsnummer(),
        til = IdentitetsnummerGenerator.identitetsnummer(),
        jobberINorge = true,
        periode = Periode("2022-01-01/2022-12-31"),
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

    internal fun overføring(periode: Periode, antallDager: Int) = SpleisetOverføringGitt(
        gjennomført = LocalDate.now(),
        gyldigFraOgMed = periode.fom,
        gyldigTilOgMed = periode.tom,
        lengde = Duration.ofDays(antallDager.toLong()),
        kilder = emptySet(),
        til = Motpart(id = "foo", type = "bar")
    )
}