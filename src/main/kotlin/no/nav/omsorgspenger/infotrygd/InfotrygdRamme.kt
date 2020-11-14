package no.nav.omsorgspenger.infotrygd

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate

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
    val barn: InfotrygdAnnenPart) : InfotrygdRamme {
    internal val barnetsFødselsdato = barn.fødselsdato
    internal val barnetsIdentitetsnummer = when (barn.type == "Identitetsnummer") {
        true -> barn.id
        false -> null
    }
}

/**
 * Rammemelding i Infotrygd for alene om omsorgen.
 */

internal data class InfotrygdAleneOmOmsorgenMelding(
    override val periode: Periode,
    override val vedtatt: LocalDate,
    override val kilder: Set<Kilde>,
    val barn: InfotrygdAnnenPart) : InfotrygdRamme

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
 */
internal data class InfotrygdOverføringFårMelding(
        override val periode: Periode,
        override val vedtatt: LocalDate,
        override val kilder: Set<Kilde>,
        val fra: InfotrygdAnnenPart,
        val lengde: Duration) : InfotrygdRamme

internal data class InfotrygdOverføringGirMelding(
        override val periode: Periode,
        override val vedtatt: LocalDate,
        override val kilder: Set<Kilde>,
        val til: InfotrygdAnnenPart,
        val lengde: Duration) : InfotrygdRamme

internal data class InfotrygdAnnenPart(
    internal val id: String,
    internal val fødselsdato: LocalDate,
    internal val type: String) {
    internal companion object {
        internal fun JSONObject.somInfotrygdAnnenPart() = InfotrygdAnnenPart(
            id = getString("id"),
            type = getString("type").let { when(it) {
                "PersonIdent" -> "Identitetsnummer"
                "Fødselsdato" -> "Fødselsdato"
                else -> throw IllegalStateException("Ugyldig 'type' $it")
            }},
            fødselsdato = LocalDate.parse(getString("fødselsdato"))
        )
    }
}