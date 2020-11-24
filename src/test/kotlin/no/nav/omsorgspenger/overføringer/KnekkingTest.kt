package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.Oslo
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.Barn.Companion.sisteDatoMedOmsorgenForOgAleneOm
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

internal class KnekkingTest {

    @Test
    fun `Om en opplysning starter før mottaksdato blir det alikevell et knekkpunkt sammen med mottaksdato`() {
        val mottaksdato = LocalDate.now()

        val barn = listOf(Barn(
            identitetsnummer = "11111111113",
            fødselsdato = mottaksdato.minusMonths(6),
            utvidetRett = false,
            aleneOmOmsorgen = true
        ))

        val overordnetPeriode = Periode(
            fom = mottaksdato,
            tom = barn.sisteDatoMedOmsorgenForOgAleneOm()!!.first
        )

        val fordelingGir = listOf(FordelingGirMelding(
            periode = Periode(
                fom = mottaksdato.minusMonths(1),
                tom = mottaksdato.plusYears(20)
            ),
            lengde = Duration.ofDays(3),
            kilder = setOf()
        ))

        val grunnlag = grunnlag(
            mottaksdato = mottaksdato,
            fordelingGir = fordelingGir,
            barn = barn
        )
        val forventetStartetGrunnet = listOf(
            Knekkpunkt.Mottaksdato,
            Knekkpunkt.OmsorgenForEtBarnStarter,
            Knekkpunkt.FordelingGirStarter
        )
        val forventetSlutterGrunnet = listOf(
            Knekkpunkt.OmsorgenForSlutter,
            Knekkpunkt.OmsorgenForEtBarnSlutter,
            Knekkpunkt.FordelingGirSlutter
        )
        val knektePerioder = grunnlag.knekk(overordnetPeriode)
        assertEquals(1, knektePerioder.size)
        assertEquals(knektePerioder.first().periode, overordnetPeriode)
        assertThat(knektePerioder.first().starterGrunnet).hasSameElementsAs(forventetStartetGrunnet)
        assertThat(knektePerioder.first().slutterGrunnet).hasSameElementsAs(forventetSlutterGrunnet)
    }


    private companion object {
        private fun grunnlag(
            mottaksdato: LocalDate,
            barn: List<Barn>,
            utvidetRett: List<UtvidetRettVedtak> = listOf(),
            fordelingGir: List<FordelingGirMelding> = listOf(),
            midlertidigAlene: List<MidlertidigAleneVedtak> = listOf()
        ) = Grunnlag(
            overføreOmsorgsdager = OverføreOmsorgsdagerMelding.Behovet(
                overførerFra = "11111111111",
                overførerTil = "11111111112",
                barn = barn,
                jobberINorge = true,
                sendtPerBrev = false,
                journalpostIder = setOf(),
                relasjon = OverføreOmsorgsdagerMelding.Relasjon.NåværendeEktefelle,
                harBoddSammentMinstEttÅr = null,
                mottatt = ZonedDateTime.of(mottaksdato, LocalTime.now(Oslo), Oslo),
                omsorgsdagerTattUtIÅr = 0,
                omsorgsdagerÅOverføre = 10
            ),
            fordelingGirMeldinger = fordelingGir,
            utvidetRettVedtak = utvidetRett,
            midlertidigAleneVedtak = midlertidigAlene
        )
    }
}