package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.formidling.MeldingsbestillingTest.Companion.barn
import no.nav.omsorgspenger.overføringer.formidling.MeldingsbestillingTest.Companion.meldingsbestillinger
import no.nav.omsorgspenger.overføringer.formidling.MeldingsbestillingTest.Companion.til
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MeldingsbestillingAvslagTest {

    @Test
    fun `Har ikke omsorgen for noen barn`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 10,
            listOf(barn()),
            relasjoner = setOf(VurderRelasjonerMelding.Relasjon(
                relasjon = "INGEN",
                identitetsnummer = til,
                borSammen = true
            ))
        )

        meldingsbestillinger.assertAvslag(Grunn.IKKE_OMSORGEN_FOR_NOEN_BARN)
    }

    @Test
    fun `Bor ikke sammen mottaker`() {
        val barn = barn()
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 10,
            listOf(barn),
            relasjoner = setOf(VurderRelasjonerMelding.Relasjon(
                relasjon = "BARN",
                identitetsnummer = barn.identitetsnummer,
                borSammen = true
            ))
        )
        meldingsbestillinger.assertAvslag(Grunn.IKKE_SAMME_ADRESSE_SOM_MOTTAKER)
    }

    @Test
    fun `Hverken bor med mottaker eller har omsorgen for noen barn`() {
        val barn = barn()
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 0,
            girDager = 10,
            listOf(barn),
            relasjoner = emptySet()
        )
        meldingsbestillinger.assertAvslag(Grunn.IKKE_SAMME_ADRESSE_SOM_MOTTAKER)
    }

    private fun List<Meldingsbestilling>.assertAvslag(avslagsGrunn: Grunn) {
        assertThat(this).hasSize(1)
        val forventetStartOgSluttGrunn = avslagsGrunn to avslagsGrunn
        val gitt = this.first { it.melding is GittDager }.melding as GittDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertTrue(gitt.formidlingsoverføringer.avslått)
        this.forEach { println(it.keyValue.second) }
    }
}