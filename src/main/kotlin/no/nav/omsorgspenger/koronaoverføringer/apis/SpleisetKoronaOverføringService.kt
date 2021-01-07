package no.nav.omsorgspenger.koronaoverføringer.apis

import KoronaoverføringRepository
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.saksnummer
import no.nav.omsorgspenger.saksnummer.SaksnummerService

/**
 * TODO: Spleiser nå ikke sammen med Overføringer fra Infotrygd.
 *  - Henter kun fra ny løsning.
 */
internal class SpleisetKoronaOverføringService(
    private val koronaoverføringRepository: KoronaoverføringRepository,
    private val saksnummerService: SaksnummerService) {
    internal fun hentSpleisetOverføringer(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : SpleisetOverføringer {

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer
        ) ?: return SpleisetOverføringer.ingenOverføringer()

        val gjeldendeOverføringer = koronaoverføringRepository.hentAlleOverføringer(setOf(saksnummer))

        if (gjeldendeOverføringer.isEmpty()) return SpleisetOverføringer.ingenOverføringer()

        require(gjeldendeOverføringer.keys == setOf(saksnummer))

        val saksnummerIdentitetsnummerMapping = saksnummerService.hentSaksnummerIdentitetsnummerMapping(
            saksnummer = gjeldendeOverføringer.saksnummer()
        )

        return SpleisetOverføringer.fraNyLøsning(
            overføringerINyLøsning = gjeldendeOverføringer.getValue(saksnummer),
            saksnummerIdentitetsnummerMapping = saksnummerIdentitetsnummerMapping,
            periode = periode
        )
    }
}