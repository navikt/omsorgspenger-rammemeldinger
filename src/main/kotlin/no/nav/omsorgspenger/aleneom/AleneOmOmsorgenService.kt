package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.infotrygd.InfotrygdAleneOmOmsorgenMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import java.time.LocalDate

internal class AleneOmOmsorgenService(
    private val infotrygdRammeService: InfotrygdRammeService,
    private val saksnummerService: SaksnummerService,
    private val aleneOmOmsorgenRepository: AleneOmOmsorgenRepository) {

    internal fun hentSpleisetAleneOmOmsorgen(
            identitetsnummer: Identitetsnummer,
            periode: Periode,
            correlationId: CorrelationId): List<SpleisetAleneOmOmsorgen> {
         val aleneOmOmsorgenFraInfotrygd = infotrygdRammeService.hentAleneOmOmsorgen(
                identitetsnummer = identitetsnummer,
                periode = periode,
                correlationId = correlationId
        )

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer
        )

        val aleneOmOmsorgenFraNyLøsning = when (saksnummer) {
            null -> setOf()
            else -> aleneOmOmsorgenRepository.hent(
                saksnummer = saksnummer
            ).filter { it.periode.overlapperMedMinstEnDag(periode) }.toSet()
        }

        val spleisetFraNyLøsning = aleneOmOmsorgenFraNyLøsning.spleisetFraNyLøsning()
        val spleisetFraInfotrygd = aleneOmOmsorgenFraInfotrygd
            .spleisetFraInfotrygd()
            .fjernBarn(barn = spleisetFraNyLøsning.map { it.barn })

        return spleisetFraNyLøsning.plus(spleisetFraInfotrygd)
    }

    private companion object {
        private fun List<InfotrygdAleneOmOmsorgenMelding>.spleisetFraInfotrygd() = map {
            SpleisetAleneOmOmsorgen(
                gjennomført = it.vedtatt,
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                barn = SpleisetAleneOmOmsorgen.Barn(
                    id = it.barn.id,
                    type = it.barn.type,
                    fødselsdato = it.barn.fødselsdato
                ),
                kilder = it.kilder
            )
        }
        private fun Set<AleneOmOmsorgen>.spleisetFraNyLøsning() = map {
            SpleisetAleneOmOmsorgen(
                gjennomført = it.registrert.toLocalDateOslo(),
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                barn = SpleisetAleneOmOmsorgen.Barn(
                    id = it.barn.identitetsnummer,
                    type = "Identitetsnummer",
                    fødselsdato = it.barn.fødselsdato
                ),
                kilder = setOf(it.kilde)
            )
        }
        private fun List<SpleisetAleneOmOmsorgen>.fjernBarn(barn: List<SpleisetAleneOmOmsorgen.Barn>) : List<SpleisetAleneOmOmsorgen> {
            val identitetsnummer = barn.map { it.id }
            val fødselsdatoer = barn.map { it.fødselsdato }
            return filterNot { when (it.barn.type) {
                "Identitetsnummer" -> it.barn.id in identitetsnummer
                else -> it.barn.fødselsdato in fødselsdatoer
            }}
        }
    }
}

internal data class SpleisetAleneOmOmsorgen(
    val gjennomført: LocalDate,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val barn: Barn,
    val kilder: Set<Kilde>) {
    internal data class Barn(
        val id: String,
        val type: String,
        val fødselsdato: LocalDate
    )
}