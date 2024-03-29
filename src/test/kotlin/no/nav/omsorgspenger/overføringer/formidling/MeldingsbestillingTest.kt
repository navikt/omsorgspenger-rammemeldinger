package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring
import no.nav.omsorgspenger.overføringer.Vurderinger.vurderGrunnlag
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.overføringer.formidling.Formidling.opprettMeldingsBestillinger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.Enhetstype
import no.nav.omsorgspenger.personopplysninger.Navn
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

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
    fun `Full innvilgelse ikke utvidet rett - yngste barnet uten alene om omsorgen`() {
        val meldingsbestillinger = meldingsbestillinger(
            mottatt = ZonedDateTime.parse("2021-09-29T14:15:00.000Z"),
            tattUtIÅr = 0,
            girDager = 5,
            barn = listOf(
                barn(fødselsdato = LocalDate.parse("2018-02-15"), aleneOmOmsorgen = true),
                barn(fødselsdato = LocalDate.parse("2020-03-20"), aleneOmOmsorgen = false)
            )
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
            tattUtIÅr = 14,
            girDager = 5,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(5),
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
            tattUtIÅr = 15,
            girDager = 10,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(5),
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
    fun `Ikke brukt dager i år, men fordeler dager hele perioden - delvis`() = testFordeling(
        tattUtIÅr = 0,
        girDager = 6,
        fordelingsperiode = Periode("2010-01-01/2050-12-31"),
        fordelerDager = 5,
        forventetDelvisInnvilgedeOverføringer = 1,
        forventetInnvilgedeOverføringer = 0,
        forventetAvslåtteOverføringer = 0
    )

    @Test
    fun `Ikke brukt dager i år, men fordeler alle dager ut året - delvis`() = testFordeling(
        tattUtIÅr = 0,
        girDager = 10,
        fordelingsperiode = Periode("2010-01-01/$iÅr-12-31"),
        fordelerDager = 10,
        forventetDelvisInnvilgedeOverføringer = 0,
        forventetInnvilgedeOverføringer = 1,
        forventetAvslåtteOverføringer = 1
    )

    @Test
    fun `Ikke brukt dager i år, men fordeler noen dager ut året - delvis`() = testFordeling(
        tattUtIÅr = 0,
        girDager = 10,
        fordelingsperiode = Periode("1999-01-01/$iÅr-12-31"),
        fordelerDager = 3,
        forventetDelvisInnvilgedeOverføringer = 1,
        forventetInnvilgedeOverføringer = 1,
        forventetAvslåtteOverføringer = 0
    )

    @Test
    fun `Brukt og fordelt dager i år - delvis også fra neste år pga fordeling`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 14,
            girDager = 6,
            fordelinger = listOf(FordelingGirMelding(
                periode = Periode("1999-01-01/2050-12-31"),
                lengde = Duration.ofDays(5),
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

    @Test
    fun `Koronaoverført i 2021 - full innvilgelse`() {
        val meldingsbestillinger = meldingsbestillinger(
            mottatt = ZonedDateTime.parse("$iÅr-01-15T14:15:00.000Z"),
            tattUtIÅr = 0,
            girDager = 10,
            koronaOverføringer = listOf(SpleisetOverføringGitt(
                gjennomført = LocalDate.now(),
                gyldigFraOgMed = LocalDate.parse("2021-01-01"),
                gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                lengde = Duration.ofDays(30),
                kilder = setOf(),
                til = Motpart(id = "foo", type = "bar")
            )),
            barn = listOf(barn())
        )

        meldingsbestillinger.assertInnvilgelse(
            forventetStartOgSluttGrunn = Grunn.MOTTAKSDATO to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        )
    }

    @Test
    fun `Koronaoverført i 2021 - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            mottatt = ZonedDateTime.parse("2021-01-15T14:15:00.000Z"),
            tattUtIÅr = 0,
            girDager = 10,
            koronaOverføringer = listOf(SpleisetOverføringGitt(
                gjennomført = LocalDate.now(),
                gyldigFraOgMed = LocalDate.parse("2021-01-01"),
                gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                lengde = Duration.ofDays(32),
                kilder = setOf(),
                til = Motpart(id = "foo", type = "bar")
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Koronaoverført i 2021 og brukt i år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            mottatt = ZonedDateTime.parse("2021-01-15T14:15:00.000Z"),
            tattUtIÅr = 2,
            girDager = 10,
            koronaOverføringer = listOf(SpleisetOverføringGitt(
                gjennomført = LocalDate.parse("2021-12-15"),
                gyldigFraOgMed = LocalDate.parse("2021-01-01"),
                gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                lengde = Duration.ofDays(32),
                kilder = setOf(),
                til = Motpart(id = "foo", type = "bar")
            )),
            barn = listOf(barn())
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_NOEN_DAGER_I_ÅR_OG_PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        assertThat(gitt.formidlingsoverføringer.alleOverføringer).hasSize(2)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    @Test
    fun `Koronaoverført i 2021 - avslag i 2021`() {
        val meldingsbestillinger = meldingsbestillinger(
            mottatt = ZonedDateTime.parse("2021-01-15T14:15:00.000Z"),
            tattUtIÅr = 0,
            girDager = 10,
            koronaOverføringer = listOf(SpleisetOverføringGitt(
                gjennomført = LocalDate.now(),
                gyldigFraOgMed = LocalDate.parse("2021-01-01"),
                gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                lengde = Duration.ofDays(40),
                kilder = setOf(),
                til = Motpart(id = "foo", type = "bar")
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

    private fun testFordeling(
        mottatt: ZonedDateTime = ZonedDateTime.now().minusYears(1).plusDays(1),
        tattUtIÅr: Int,
        girDager: Int,
        fordelingsperiode: Periode,
        fordelerDager: Int,
        forventetInnvilgedeOverføringer: Int,
        forventetDelvisInnvilgedeOverføringer: Int,
        forventetAvslåtteOverføringer: Int) {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = tattUtIÅr,
            girDager = girDager,
            fordelinger = listOf(FordelingGirMelding(
                periode = fordelingsperiode,
                lengde = Duration.ofDays(fordelerDager.toLong()),
                kilder = setOf()
            )),
            barn = listOf(barn()),
            mottatt = mottatt
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.PÅGÅENDE_FORDELING to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gittDager = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottattDager= meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gittDager.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottattDager.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gittDager.formidlingsoverføringer.innvilget)
        assertFalse(gittDager.formidlingsoverføringer.avslått)
        assertThat(gittDager.formidlingsoverføringer.innvilgedeOverføringer).hasSize(forventetInnvilgedeOverføringer)
        assertThat(gittDager.formidlingsoverføringer.delvisInnvilgedeOverføringer).hasSize(forventetDelvisInnvilgedeOverføringer)
        assertThat(gittDager.formidlingsoverføringer.avslåtteOverføringer).hasSize(forventetAvslåtteOverføringer)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }

    internal companion object {
        private val iÅr = LocalDate.now().year.toString()
        private val enhet = Enhet(enhetsnummer = "1234", enhetstype = Enhetstype.VANLIG)
        private const val fra = "11111111111"
        internal const val til = "22222222222"
        private const val tidligerePartner = "44444444444"

        internal fun barn(
            fødselsdato: LocalDate = LocalDate.parse("2020-12-15"),
            utvidetRett: Boolean = false,
            aleneOmOmsorgen: Boolean = true) = Barn(
            fødselsdato = fødselsdato,
            identitetsnummer = IdentitetsnummerGenerator.identitetsnummer(),
            aleneOmOmsorgen = aleneOmOmsorgen,
            utvidetRett = utvidetRett
        )
        internal fun meldingsbestillinger(
            tattUtIÅr: Int,
            girDager: Int,
            barn: List<Barn>,
            fordelinger: List<FordelingGirMelding> = listOf(),
            koronaOverføringer: List<SpleisetOverføringGitt> = listOf(),
            relasjoner: Set<VurderRelasjonerMelding.Relasjon> = barn.map { VurderRelasjonerMelding.Relasjon(
                relasjon = "BARN",
                identitetsnummer = it.identitetsnummer,
                borSammen = true
            )}.plus(VurderRelasjonerMelding.Relasjon(
                relasjon = "INGEN",
                identitetsnummer = til,
                borSammen = true
            )).toSet(),
            medTidligerePartner: Boolean = false,
            mottatt: ZonedDateTime = ZonedDateTime.now()
        ) : List<Meldingsbestilling> {
            val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.Behovet(
                overførerFra = fra,
                overførerTil = til,
                jobberINorge = true,
                sendtPerBrev = false,
                mottatt = mottatt,
                journalpostIder = setOf(),
                relasjon = OverføreOmsorgsdagerMelding.Relasjon.NåværendeSamboer,
                harBoddSammentMinstEttÅr = true,
                omsorgsdagerTattUtIÅr = tattUtIÅr,
                omsorgsdagerÅOverføre = girDager,
                barn = barn
            )

            val behandling = Behandling(
                sendtPerBrev = overføreOmsorgsdager.sendtPerBrev,
                periode = overføreOmsorgsdager.overordnetPeriode
            )

            val grunnlag = vurderGrunnlag(
                grunnlag = Grunnlag(
                    overføreOmsorgsdager = overføreOmsorgsdager,
                    utvidetRettVedtak = barn.filter { it.utvidetRett }.map { UtvidetRettVedtak(
                        periode = overføreOmsorgsdager.overordnetPeriode,
                        barnetsFødselsdato = it.fødselsdato,
                        barnetsIdentitetsnummer = it.identitetsnummer,
                        kilder = setOf()
                    )},
                    midlertidigAleneVedtak = listOf(),
                    fordelingGirMeldinger = fordelinger,
                    koronaOverføringer = koronaOverføringer,
                    relasjoner = relasjoner
                ),
                behandling = behandling
            )

            val nyeOverføringer = beregnOmsorgsdagerTilgjengeligForOverføring(
                grunnlag = grunnlag,
                behandling = behandling
            ).somNyeOverføringer(overføreOmsorgsdager.omsorgsdagerÅOverføre)

            val alleSaksnummerMapping = mapOf(
                fra to "1",
                til to "2"
            ).let { when (medTidligerePartner){
                true -> it.plus(tidligerePartner to "3")
                false -> it
            }}

            return opprettMeldingsBestillinger(
                behovssekvensId = "foo",
                personopplysninger = mapOf(
                    fra to Personopplysninger(gjeldendeIdentitetsnummer = fra, fødselsdato = LocalDate.now(),
                        navn = Navn("Ola","En","Nordmann"), aktørId = "123", adressebeskyttet = false, enhet = enhet),
                    til to Personopplysninger(gjeldendeIdentitetsnummer = til, fødselsdato = LocalDate.now(),
                        navn = Navn("Kari","To", "Nordmann"), aktørId = "345", adressebeskyttet = false, enhet = enhet),
                    tidligerePartner to  Personopplysninger(gjeldendeIdentitetsnummer = til, fødselsdato = LocalDate.now(),
                        navn = Navn("Heidi","Tre", "Nordmann"), aktørId = "789", adressebeskyttet = false, enhet = enhet)
                ),
                overføreOmsorgsdager = overføreOmsorgsdager,
                behandling = OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling(
                    karakteristikker = behandling.karakteristikker(),
                    overføringer = nyeOverføringer,
                    gjeldendeOverføringer = mapOf(),
                    alleSaksnummerMapping = alleSaksnummerMapping,
                    berørteSaksnummer = alleSaksnummerMapping.values.toSet(),
                    periode = behandling.periode
                )
            )
        }
    }
}