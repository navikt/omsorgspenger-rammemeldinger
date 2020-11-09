package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringFårMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringGirMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdRamme
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import java.time.Duration
import java.time.LocalDate

internal class OverføringService(
    private val infotrygdRammeService: InfotrygdRammeService,
    private val saksnummerService: SaksnummerService,
    private val overføringRepository: OverføringRepository) {

    internal fun hentSpleisetOverføringer(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : SpleisetOverføringer {

        val gittIInfotrygd = infotrygdRammeService.hentOverføringGir(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )

        val fåttIInfotrygd = infotrygdRammeService.hentOverføringFår(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )

        val sistOverførtIInfotrygd = gittIInfotrygd.plus(fåttIInfotrygd).sistVedtatt()

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = identitetsnummer
        )

        val overføringerINyLøsning = when (saksnummer) {
            null -> mapOf()
            else -> overføringRepository.hentOverføringer(
                saksnummer = setOf(saksnummer)
            )
        }

        val saksnummerIdentitetsnummerMapping = saksnummerService.hentSaksnummerIdentitetsnummerMapping(
            saksnummer = overføringerINyLøsning.saksnummer()
        )

        val sistOverførtINyLøsning = overføringerINyLøsning[saksnummer]?.sistGjennomført()

        return when {
            sistOverførtIInfotrygd == null && sistOverførtINyLøsning == null -> SpleisetOverføringer.ingenOverføringer()
            sistOverførtIInfotrygd?.isAfter(sistOverførtINyLøsning)?:false -> SpleisetOverføringer.fraInfotrygd(
                gitt = gittIInfotrygd,
                fått = fåttIInfotrygd
            )
            else -> SpleisetOverføringer.fraNyLøsning(
                overføringerINyLøsning = overføringerINyLøsning.getValue(saksnummer!!),
                saksnummerIdentitetsnummerMapping = saksnummerIdentitetsnummerMapping,
                periode = periode
            )
        }

    }
}

private fun Collection<InfotrygdRamme>.sistVedtatt() = minByOrNull { it.vedtatt }?.vedtatt
private fun GjeldendeOverføringer.sistGjennomført() : LocalDate? {
    val sistGitt = gitt.minByOrNull { it.gjennomført }?.gjennomført
    val sistFått = fått.minByOrNull { it.gjennomført }?.gjennomført
    return when {
        sistGitt == null && sistFått == null -> null
        sistGitt?.isAfter(sistFått)?:false -> sistGitt!!.toLocalDate()
        else -> sistFått!!.toLocalDate()
    }
}
internal data class SpleisetOverføringer(
    val gitt: List<SpleisetOverføringGitt>,
    val fått: List<SpleisetOverføringFått>) {
    internal companion object {
        internal fun ingenOverføringer() = SpleisetOverføringer(
            gitt = emptyList(),
            fått = emptyList()
        )
        internal fun fraInfotrygd(
            gitt: List<InfotrygdOverføringGirMelding>,
            fått: List<InfotrygdOverføringFårMelding>) = SpleisetOverføringer(
            gitt = gitt.map { SpleisetOverføringGitt(
                gjennomført = it.vedtatt,
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                til = Motpart(
                    id = it.til.id,
                    type = it.til.type
                ),
                lengde = it.lengde,
                kilder = it.kilder
            )},
            fått = fått.map { SpleisetOverføringFått(
                gjennomført = it.vedtatt,
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                fra = Motpart(
                    id = it.fra.id,
                    type = it.fra.type
                ),
                lengde = it.lengde,
                kilder = it.kilder
            )}
        )
        internal fun fraNyLøsning(
            overføringerINyLøsning: GjeldendeOverføringer,
            saksnummerIdentitetsnummerMapping: Map<Saksnummer, Identitetsnummer>,
            periode: Periode
        ) = SpleisetOverføringer(
            gitt = overføringerINyLøsning.gitt.filter { it.periode.overlapperMedMinstEnDag(periode) }.map { SpleisetOverføringGitt(
                gjennomført = it.gjennomført.toLocalDateOslo(),
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                til = Motpart(
                    id = saksnummerIdentitetsnummerMapping.getValue(it.til)
                ),
                lengde = Duration.ofDays(it.antallDager.toLong()),
                kilder = setOf(Kilde(id = "TODO", type = "OmsorgspengerRammemeldinger"))
            )},
            fått = overføringerINyLøsning.fått.filter { it.periode.overlapperMedMinstEnDag(periode) }.map { SpleisetOverføringFått(
                gjennomført = it.gjennomført.toLocalDateOslo(),
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                fra = Motpart(
                    id = saksnummerIdentitetsnummerMapping.getValue(it.fra)
                ),
                lengde = Duration.ofDays(it.antallDager.toLong()),
                kilder = setOf(Kilde(id = "TODO", type = "OmsorgspengerRammemeldinger"))
            )}
        )
    }
}

internal data class SpleisetOverføringGitt(
    val gjennomført: LocalDate,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val til: Motpart,
    val lengde: Duration,
    val kilder: Set<Kilde>
)

internal data class SpleisetOverføringFått(
    val gjennomført: LocalDate,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val fra: Motpart,
    val lengde: Duration,
    val kilder: Set<Kilde>
)

internal data class Motpart(
    val id: String,
    val type: String = "Identitetsnummer"
)


