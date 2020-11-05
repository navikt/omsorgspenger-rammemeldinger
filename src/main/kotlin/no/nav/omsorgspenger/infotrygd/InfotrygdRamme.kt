package no.nav.omsorgspenger.infotrygd

import no.nav.omsorgspenger.AnnenPart
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal interface InfotrygdRamme{
    val periode: Periode
    val vedtatt: LocalDate
    val kilder: Set<Kilde>
}

/**
 * Rammevedtak i Infotrygd for utvidet rett.
 *  TODO: Bør hentes fra `omsorgspenger-rammevedtak`
 */
internal data class InfotrygdUtvidetRettVedtak(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>,
    internal val barnetsFødselsdato: LocalDate,
    internal val barnetsIdentitetsnummer: Identitetsnummer? = null) : InfotrygdRamme

/**
 * Rammemelding i Infotrygd for alene om omsorgen.
 *  TODO: Inntil videre bruker vi ikke denne til noe,
 *        men når vi skal tilby API for alene om omsorgen må også disse hensynstas.
 *        I første omgang kun "proxye" det som finnes i Infotrygd
 *        https://github.com/navikt/omsorgspenger-rammemeldinger/issues/23
 */

internal data class InfotrygdAleneOmOmsorgenMelding(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>,
    internal val barnetsFødselsdato: LocalDate,
    internal val barnetsIdentitetsnummer: Identitetsnummer? = null) : InfotrygdRamme {

    val barn: InfotrygdAnnenPart
        get() {
            if(barnetsIdentitetsnummer != null) {
                return InfotrygdAnnenPart(
                        id = barnetsIdentitetsnummer,
                        type = "Identitetsnummer",
                        fødselsdato = barnetsFødselsdato
                )
            } else {
                val ZONE_ID = ZoneId.of("Europe/Oslo")
                val ddMMyy = DateTimeFormatter.ofPattern("ddMMyy").withZone(ZONE_ID)

                return InfotrygdAnnenPart(
                        id = ddMMyy.format(barnetsFødselsdato),
                        fødselsdato = barnetsFødselsdato,
                        type = "Fødselsdato"
                )
            }
        }
}

/**
 * Rammevedtak i Infotrygd for midlertidig alene om omsorgen.
 *  TODO: Bør hentes fra `omsorgspenger-rammevedtak`
 */
internal data class InfotrygdMidlertidigAleneVedtak(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>) : InfotrygdRamme

/**
 * Rammemeldinger i Infotrygd for fordelinger.
 *  TODO: Inntil videre bruker vi kun fordeling gir,
 *        men når vi skal tilby API for fordelinger må også disse hensynstas.
 *        I første omgang kun "proxye" det som finnes i Infotrygd
 */

internal data class InfotrygdFordelingFårMelding(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>,
    internal val lengde: Duration) : InfotrygdRamme

internal data class InfotrygdFordelingGirMelding(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>,
    internal val lengde: Duration) : InfotrygdRamme

/**
 * Rammemeldinger i Infotrygd for overføringer.
 *  TODO: Inntil videre bruker vi ikke disse til noe,
 *        men når vi skal tilby API for overføringer må også disse hensynstas
 *        https://github.com/navikt/omsorgspenger-rammemeldinger/issues/23
 */
internal data class InfotrygdOverføringFårMelding(
        override val periode: Periode,
        override val vedtatt: LocalDate,
        override val kilder: Set<Kilde>,
        val barn: InfotrygdAnnenPart,
        internal val lengde: Duration) : InfotrygdRamme

internal data class InfotrygdOverføringGirMelding(
        override val periode: Periode,
        override val vedtatt: LocalDate,
        override val kilder: Set<Kilde>,
        val barn: InfotrygdAnnenPart,
        internal val lengde: Duration) : InfotrygdRamme

internal data class InfotrygdAnnenPart(override val id: Identitetsnummer, override val fødselsdato: LocalDate, override val type: String) : AnnenPart