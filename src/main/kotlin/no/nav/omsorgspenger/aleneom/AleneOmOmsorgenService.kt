package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.infotrygd.InfotrygdAleneOmOmsorgenMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdAnnenPart
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
         ).toMutableList()

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer
        )

        val aleneOmOmsorgenFraNyLøsning = when (saksnummer) {
            null -> setOf()
            else -> aleneOmOmsorgenRepository.hent(
                saksnummer = saksnummer
            ).filter { it.periode.overlapperMedMinstEnDag(periode) }.toSet()
        }

        val spleiset = mutableListOf<SpleisetAleneOmOmsorgen>()

        aleneOmOmsorgenFraNyLøsning.forEach { fraNyLøsning ->
            val forSammeBarnIInfotrygd = aleneOmOmsorgenFraInfotrygd
                .firstOrNull { fraNyLøsning.barn.erSamme(it.barn) }
            when (forSammeBarnIInfotrygd) {
                null -> spleiset.add(fraNyLøsning.somSpleiset())
                else -> {
                    aleneOmOmsorgenFraInfotrygd.remove(forSammeBarnIInfotrygd)
                    spleiset.add((fraNyLøsning to forSammeBarnIInfotrygd).spleis())
                }
            }
        }
        aleneOmOmsorgenFraInfotrygd.forEach { fraInfotrygd ->
            spleiset.add(fraInfotrygd.somSpleiset())
        }

        return spleiset.toList()
    }

    private companion object {
        private const val IdentitetsnummerType = "Identitetsnummer"

        private fun AleneOmOmsorgen.somSpleiset() = SpleisetAleneOmOmsorgen(
            registrert = registrert.toLocalDateOslo(),
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            barn = SpleisetAleneOmOmsorgen.Barn(
                id = barn.identitetsnummer,
                type = "Identitetsnummer",
                fødselsdato = barn.fødselsdato
            ),
            kilder = setOf(kilde)
        )

        private fun InfotrygdAleneOmOmsorgenMelding.somSpleiset() = SpleisetAleneOmOmsorgen(
            registrert = vedtatt,
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            barn = SpleisetAleneOmOmsorgen.Barn(
                id = barn.id,
                type = barn.type,
                fødselsdato = barn.fødselsdato
            ),
            kilder = kilder
        )

        private fun Pair<AleneOmOmsorgen, InfotrygdAleneOmOmsorgenMelding>.spleis() : SpleisetAleneOmOmsorgen {
            val (fraNyLøsning, fraInfotrygd) = this
            val registrertNyLøsning = fraNyLøsning.registrert.toLocalDateOslo()
            return SpleisetAleneOmOmsorgen(
                registrert = when (fraInfotrygd.vedtatt.isBefore(registrertNyLøsning)) {
                    true -> fraInfotrygd.vedtatt
                    false -> registrertNyLøsning
                },
                barn = SpleisetAleneOmOmsorgen.Barn(
                    id = fraInfotrygd.barn.id,
                    type = fraInfotrygd.barn.type,
                    fødselsdato = fraNyLøsning.barn.fødselsdato
                ),
                kilder = fraInfotrygd.kilder.plus(fraNyLøsning.kilde),
                gyldigFraOgMed = when (fraInfotrygd.periode.fom.isBefore(fraNyLøsning.periode.fom)) {
                    true -> fraInfotrygd.periode.fom
                    false -> fraNyLøsning.periode.fom
                },
                gyldigTilOgMed = when (fraInfotrygd.periode.tom.isAfter(fraNyLøsning.periode.tom)) {
                    true -> fraInfotrygd.periode.tom
                    false -> fraNyLøsning.periode.tom
                }
            )
        }

        private fun AleneOmOmsorgen.Barn.erSamme(
            fraInfotrygd: InfotrygdAnnenPart) = when (fraInfotrygd.type == IdentitetsnummerType) {
            true -> identitetsnummer == fraInfotrygd.id
            false -> fødselsdato.isEqual(fraInfotrygd.fødselsdato)
        }
    }
}

internal data class SpleisetAleneOmOmsorgen(
    val registrert: LocalDate,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val barn: Barn,
    val kilder: Set<Kilde>) {
    @Deprecated(message = "Bruk registrert") val gjennomført = registrert
    internal data class Barn(
        val id: String,
        val type: String,
        val fødselsdato: LocalDate
    )
}