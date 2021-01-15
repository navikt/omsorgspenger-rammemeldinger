package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.overføringer.formidling.MeldingsbestillingTest.Companion.meldingsbestillinger
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class MeldingsbestillingAvslagTest {

    @Test
    fun `Brukt alle dager i år - delvis`() {
        val meldingsbestillinger = meldingsbestillinger(
            tattUtIÅr = 20,
            girDager = 5,
            listOf(MeldingsbestillingTest.barn())
        )

        Assertions.assertThat(meldingsbestillinger).hasSize(2)
        val forventetStartOgSluttGrunn = Grunn.BRUKT_ALLE_DAGER_I_ÅR to Grunn.OMSORGEN_FOR_BARN_OPPHØRER
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.melding as GittDager
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.melding as MottattDager
        assertEquals(gitt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertEquals(mottatt.formidlingsoverføringer.startOgSluttGrunn, forventetStartOgSluttGrunn)
        assertFalse(gitt.formidlingsoverføringer.innvilget)
        assertFalse(gitt.formidlingsoverføringer.avslått)
        meldingsbestillinger.forEach { println(it.keyValue.second) }
    }
}