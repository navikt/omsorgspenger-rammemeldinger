package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.Grunnlag.Companion.vurdert
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.barn
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.behovet
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.grunnlag
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class GrunnlagVurderingTest {

    @Test
    fun `grunnlag uten barn med utvidet rett`() {
        val behovet = behovet(
            barn = listOf(barn(), barn())
        )
        val behandling = Behandling(behovet)
        val grunnlag = grunnlag(
            behovet = behovet
        )
        assertEquals(grunnlag, grunnlag.vurdert(behandling))
        assertFalse(behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett)
    }

    @Test
    fun `grunnlag med barn med utvidet rett`() {
        val barnUtenUtvidetRett = barn()
        val barnMedUtvidetRett = barn(utvidetRett = true)
        val behovet = behovet(
            barn = listOf(barnUtenUtvidetRett, barnMedUtvidetRett)
        )
        val behandling = Behandling(behovet)
        val grunnlag = grunnlag(
            behovet = behovet,
            utvidetRett = listOf(UtvidetRettVedtak(
                periode = behovet.periode,
                barnetsFødselsdato = barnMedUtvidetRett.fødselsdato,
                kilder = emptySet()
            ))
        )
        assertEquals(grunnlag, grunnlag.vurdert(behandling))
        assertFalse(behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett)
    }

    @Test
    fun `grunnlag med barn med utvidet rett som ikke kan verifiseres`() {
        val barnUtenUtvidetRett = barn()
        val barnMedUtvidetRett = barn(utvidetRett = true)
        val behovet = behovet(
            barn = listOf(barnUtenUtvidetRett, barnMedUtvidetRett)
        )
        val behandling = Behandling(behovet)
        val grunnlag = grunnlag(
            behovet = behovet
        )
        val vurdertGrunnlag = grunnlag.vurdert(behandling)
        assertFalse(grunnlag == vurdertGrunnlag)
        assertEquals(2, vurdertGrunnlag.behovet.barn.size)
        assertThat(vurdertGrunnlag.behovet.barn).contains(barnUtenUtvidetRett)
        assertThat(vurdertGrunnlag.behovet.barn).contains(barnMedUtvidetRett.copy(utvidetRett = false))
        assertTrue(behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett)
    }
}