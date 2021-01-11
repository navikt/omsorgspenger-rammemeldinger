package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import java.time.LocalDate

internal class AleneOmOmsorgenService(
    private val aleneOmOmsorgenRepository: AleneOmOmsorgenRepository) {

    internal fun lagreIForbindelseMedOverføring(
        behovssekvensId: BehovssekvensId,
        saksnummer: Saksnummer,
        dato: LocalDate,
        aleneOmOmsorgenFor: List<AleneOmOmsorgen.Barn>) = lagre(
        behovssekvensId = behovssekvensId,
        saksnummer = saksnummer,
        dato = dato,
        aleneOmOmsorgenFor = aleneOmOmsorgenFor,
        registreresIForbindelseMed = AleneOmOmsorgenRepository.RegistreresIForbindelseMed.Overføring
    )

    internal fun lagreIForbindelseMedKoronaOverføring(
        behovssekvensId: BehovssekvensId,
        saksnummer: Saksnummer,
        dato: LocalDate,
        aleneOmOmsorgenFor: List<AleneOmOmsorgen.Barn>) = lagre(
        behovssekvensId = behovssekvensId,
        saksnummer = saksnummer,
        dato = dato,
        aleneOmOmsorgenFor = aleneOmOmsorgenFor,
        registreresIForbindelseMed = AleneOmOmsorgenRepository.RegistreresIForbindelseMed.KoronaOverføring
    )

    private fun lagre(
        behovssekvensId: BehovssekvensId,
        saksnummer: Saksnummer,
        dato: LocalDate,
        aleneOmOmsorgenFor: List<AleneOmOmsorgen.Barn>,
        registreresIForbindelseMed: AleneOmOmsorgenRepository.RegistreresIForbindelseMed) {
        aleneOmOmsorgenRepository.lagre(
            behovssekvensId = behovssekvensId,
            saksnummer = saksnummer,
            registreresIForbindelseMed = registreresIForbindelseMed,
            aleneOmOmsorgenFor = aleneOmOmsorgenFor.mapNotNull {
                /**
                 * Lagrer nå alene om omsorgen ut året barnet fyller 18 uavhengig av det utvidet rett eller ikke.
                 * Dette for å unngå ev. problemer om man skulle få utvidet rett for barnet senere.
                 */
                val aleneOmOmsorgenTilOgMed = it.fødselsdato.plusYears(18).sisteDagIÅret()
                when (dato.isAfter(aleneOmOmsorgenTilOgMed)) {
                    true -> null
                    false -> AleneOmOmsorgenFor(
                        identitetsnummer = it.identitetsnummer,
                        fødselsdato = it.fødselsdato,
                        aleneOmOmsorgenI = Periode(
                            fom = dato,
                            tom = aleneOmOmsorgenTilOgMed
                        )
                    )
                }
            }.toSet()
        )
    }
}