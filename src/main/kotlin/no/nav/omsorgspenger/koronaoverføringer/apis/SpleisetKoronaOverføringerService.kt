package no.nav.omsorgspenger.koronaoverføringer.apis

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.koronaoverføringer.db.KoronaoverføringRepository
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringFått
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.saksnummer
import no.nav.omsorgspenger.saksnummer.SaksnummerService

internal class SpleisetKoronaOverføringerService(
    private val koronaoverføringRepository: KoronaoverføringRepository,
    private val saksnummerService: SaksnummerService,
    private val infotrygdRammeService: InfotrygdRammeService) {

    internal suspend fun hentSpleisetOverføringer(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : SpleisetOverføringer {

        val fraNyLøsning = fraNyLøsning(identitetsnummer, periode)
        val fraInfotrygd = fraInfotrygd(identitetsnummer, periode, correlationId)

        return SpleisetOverføringer(
            gitt = fraNyLøsning.gitt.plus(fraInfotrygd.gitt),
            fått = fraNyLøsning.fått.plus(fraInfotrygd.fått)
        )
    }

    private fun fraNyLøsning(
        identitetsnummer: Identitetsnummer,
        periode: Periode) : SpleisetOverføringer {
        if (!periode.inneholderDagerI2021() && !periode.inneholderDagerI2022()) return SpleisetOverføringer.ingenOverføringer()

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer
        ) ?: return SpleisetOverføringer.ingenOverføringer()

        val gjeldendeOverføringer = koronaoverføringRepository.hentAlleOverføringer(setOf(saksnummer))

        val gjeldendeOverføringerForEtterspurtPerson = gjeldendeOverføringer[saksnummer]
            ?: return SpleisetOverføringer.ingenOverføringer()

        val saksnummerIdentitetsnummerMapping = saksnummerService.hentSaksnummerIdentitetsnummerMapping(
            saksnummer = gjeldendeOverføringer.saksnummer()
        )

        return SpleisetOverføringer.fraNyLøsning(
            overføringerINyLøsning = gjeldendeOverføringerForEtterspurtPerson,
            saksnummerIdentitetsnummerMapping = saksnummerIdentitetsnummerMapping,
            periode = periode
        )
    }

    private suspend fun fraInfotrygd(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : SpleisetOverføringer {

        val gitt = infotrygdRammeService.hentKoronaOverføringGir(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        ).map { SpleisetOverføringGitt(
            gjennomført = it.vedtatt,
            gyldigFraOgMed = it.periode.fom,
            gyldigTilOgMed = it.periode.tom,
            til = Motpart(id = it.til.id, type = it.til.type),
            lengde = it.lengde,
            kilder = it.kilder
        )}

        val fått = infotrygdRammeService.hentKoronaOverføringFår(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        ).map { SpleisetOverføringFått(
            gjennomført = it.vedtatt,
            gyldigFraOgMed = it.periode.fom,
            gyldigTilOgMed = it.periode.tom,
            fra = Motpart(id = it.fra.id, type = it.fra.type),
            lengde = it.lengde,
            kilder = it.kilder
        )}

        return SpleisetOverføringer(gitt = gitt, fått = fått)
    }

    internal companion object {
        private val År2021 = Periode("2021-01-01/2021-12-31")
        private val År2022 = Periode("2022-01-01/2022-12-31")
        internal fun Periode.inneholderDagerI2021() = overlapperMedMinstEnDag(År2021)
        internal fun Periode.inneholderDagerI2022() = overlapperMedMinstEnDag(År2022)
    }
}