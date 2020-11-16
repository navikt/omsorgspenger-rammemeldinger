package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.formidling.Formidling.opprettMeldingsBestillinger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MeldingsbestillingTest {

    @Test
    fun `Full innvilgelse ikke utvidet rett`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 5,
            listOf(barn())
        )

        meldingsbestillinger.assertInnvilgelse(
            forventetStartOgSluttGrunn = Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        )
    }

    @Test
    fun `Full innvilgelse med utvidet rett`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 5,
            listOf(barn(utvidetRett = true))
        )

        meldingsbestillinger.assertInnvilgelse(
            forventetStartOgSluttGrunn = Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
        )
    }

    @Test
    fun `Full innvilgelse med utvidet rett og tidligere partner`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 5,
            listOf(barn(utvidetRett = true)),
            medTidligerePartner = true
        )

        assertThat(meldingsbestillinger).hasSize(3)
        val forventetStartOgSluttGrunn = Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_MED_UTVIDET_RETT_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        val tidligerePartner = meldingsbestillinger.first { it.melding is TidligerePartner}.melding as TidligerePartner
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertTrue(gitt.formidlingsoverføringer.innvilget)
        assertEquals(gitt.formidlingsoverføringer.innvilgedeOverføringer.first().periode.fom, tidligerePartner.fraOgMed)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    private fun List<Meldingsbestilling>.assertInnvilgelse(forventetStartOgSluttGrunn: Pair<Grunn, Grunn>) {
        assertThat(this).hasSize(2)
        val gitt = first { it.melding is GittDager }.melding as GittDager
        val mottatt = first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertTrue(gitt.formidlingsoverføringer.innvilget)
        forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt alle dager i år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 20,
            girDager = 5,
            listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_ALLE_DAGER_I_ÅR to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt fler dager enn man har tilgjengelig i år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 50,
            girDager = 5,
            listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_ALLE_DAGER_I_ÅR to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt noen dager i år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 15,
            girDager = 6,
            listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_NOEN_DAGER_I_ÅR to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt noe og fordelt noe i år - fortsatt dager tilgjengelig - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 8,
            girDager = 5,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(10),
                kilder = setOf()
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Fordelt alt med annen forelder - avslag`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 5,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(20),
                kilder = setOf()
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(1)
        val forventetStartOgSluttGrunn = Grunn.PÅGÅENDE_FORDELING to Grunn.PÅGÅENDE_FORDELING
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertTrue(gitt.formidlingsoverføringer.avslått)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt alle dager i år - fordeler dager så får delvis innvilget neste år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 5,
            girDager = 10,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(15),
                kilder = setOf()
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_ALLE_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Ikke brukt dager i år, men fordeler dager - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 6,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(15),
                kilder = setOf()
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(1)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Brukt og fordelt dager i år - delvis også fra neste år pga fordeling`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 4,
            girDager = 6,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(15),
                kilder = setOf()
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    private companion object {
        private const val fra = "11111111111"
        private const val til = "22222222222"
        private const val tidligerePartner = "44444444444"

        private fun barn(fødselsdato: LocalDate = LocalDate.now().minusYears(1), utvidetRett: Boolean = false) = Barn(
            fødselsdato = fødselsdato,
            identitetsnummer = "33333333333",
            aleneOmOmsorgen = true,
            utvidetRett = utvidetRett
        )
        private fun meldingsbestillinger(
            tattUtIÅr: Int,
            girDager: Int,
            barn: List<Barn>,
            fordelinger: List<FordelingGirMelding> = listOf(),
            medTidligerePartner: Boolean = false
        ) : List<Meldingsbestilling> {
            val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.Behovet(
                overførerFra = fra,
                overførerTil = til,
                jobberINorge = true,
                sendtPerBrev = false,
                mottatt = ZonedDateTime.now(),
                journalpostIder = setOf(),
                relasjon = OverføreOmsorgsdagerMelding.Relasjon.NåværendeSamboer,
                harBoddSammentMinstEttÅr = true,
                omsorgsdagerTattUtIÅr = tattUtIÅr,
                omsorgsdagerÅOverføre = girDager,
                barn = barn
            )
            val grunnlag = Grunnlag(
                overføreOmsorgsdager = overføreOmsorgsdager,
                utvidetRettVedtak = listOf(),
                midlertidigAleneVedtak = listOf(),
                fordelingGirMeldinger = fordelinger
            )

            val behandling = Behandling(
                sendtPerBrev = overføreOmsorgsdager.sendtPerBrev,
                periode = overføreOmsorgsdager.overordnetPeriode
            )

            val nyeOverføringer = beregnOmsorgsdagerTilgjengeligForOverføring(
                grunnlag = grunnlag,
                behandling = behandling
            ).somNyeOverføringer(overføreOmsorgsdager.omsorgsdagerÅOverføre)

            return opprettMeldingsBestillinger(
                behovssekvensId = "foo",
                personopplysninger = mapOf(
                    fra to Personopplysninger(gjeldendeIdentitetsnummer = fra, fødselsdato = LocalDate.now(),
                        navn = Personopplysninger.Navn("Ola","En","Nordmann"), aktørId = "123", adressebeskyttet = false),
                    til to Personopplysninger(gjeldendeIdentitetsnummer = til, fødselsdato = LocalDate.now(),
                        navn = Personopplysninger.Navn("Kari","To", "Nordmann"), aktørId = "345", adressebeskyttet = false),
                    tidligerePartner to  Personopplysninger(gjeldendeIdentitetsnummer = til, fødselsdato = LocalDate.now(),
                        navn = Personopplysninger.Navn("Heidi","Tre", "Nordmann"), aktørId = "789", adressebeskyttet = false)
                ),
                overføreOmsorgsdager = overføreOmsorgsdager,
                behandling = OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling(
                    karakteristikker = behandling.karakteristikker(),
                    overføringer = nyeOverføringer,
                    gjeldendeOverføringer = mapOf(),
                    alleSaksnummerMapping = mapOf(
                        fra to "1",
                        til to "2"
                    ).let { when (medTidligerePartner){
                        true -> it.plus(tidligerePartner to "3")
                        false -> it
                    }},
                    berørteSaksnummer = setOf("1","2","3"),
                    periode = behandling.periode
                )
            )
        }
    }
}