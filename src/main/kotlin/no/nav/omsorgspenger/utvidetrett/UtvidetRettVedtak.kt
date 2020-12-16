package no.nav.omsorgspenger.utvidetrett

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal data class UtvidetRettVedtak(
    val periode: Periode,
    val barnetsFødselsdato: LocalDate,
    val barnetsIdentitetsnummer: String? = null,
    val kilder: Set<Kilde>
)

internal object UtvidetRettVedtakVurderinger {
    private val logger = LoggerFactory.getLogger(UtvidetRettVedtakVurderinger::class.java)

    internal fun List<UtvidetRettVedtak>.inneholderRammevedtakFor(
        barnetsFødselsdato: LocalDate,
        omsorgenForBarnet: Periode,
        mottaksdato: LocalDate) =
        filter {
            it.barnetsFødselsdato.isEqual(barnetsFødselsdato)
        }
            .also { if (it.size > 1) {
                logger.warn("Fant ${it.size} utvidet rett vedtak på barn født $barnetsFødselsdato")
            }}
            .also { if (it.isNotEmpty()) {
                it.filter { vedtak ->
                    vedtak.periode.tom != omsorgenForBarnet.tom
                }.also { uforventetTom -> if(uforventetTom.isNotEmpty()) {
                    logger.warn("Fant ${uforventetTom.size} utvidet rett vedtak med 'tom' satt til noe annet enn ut året barnet fyller 18.")
                }}
                it.filter { vedtak ->
                    vedtak.periode.fom.isAfter(mottaksdato)
                }.also { uforventetFom -> if(uforventetFom.isNotEmpty()) {
                    logger.warn("Fant ${uforventetFom.size} utvidet rett vedtak med 'fom' satt til etter mottaksdato for overføringen.")
                }}
            }}
            .isNotEmpty()
}