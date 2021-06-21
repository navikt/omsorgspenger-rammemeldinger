package no.nav.omsorgspenger.overføringer.apis

import no.nav.omsorgspenger.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringFårMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringGirMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdRamme
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.overføringer.saksnummer
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import java.time.Duration
import java.time.LocalDate

internal class SpleisetOverføringerService(
    private val infotrygdRammeService: InfotrygdRammeService,
    private val saksnummerService: SaksnummerService,
    private val overføringRepository: OverføringRepository) {

    internal suspend fun hentSpleisetOverføringer(
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
            else -> overføringRepository.hentAktiveOverføringer(
                saksnummer = setOf(saksnummer)
            )
        }

        val saksnummerIdentitetsnummerMapping = saksnummerService.hentSaksnummerIdentitetsnummerMapping(
            saksnummer = overføringerINyLøsning.saksnummer()
        )

        val sistOverførtINyLøsning = overføringerINyLøsning[saksnummer]?.sistGjennomført()

        return when {
            sistOverførtIInfotrygd == null && sistOverførtINyLøsning == null -> SpleisetOverføringer.ingenOverføringer()
            sistOverførtIInfotrygd != null && sistOverførtINyLøsning != null -> when (sistOverførtIInfotrygd.isAfter(sistOverførtINyLøsning)) {
                true -> SpleisetOverføringer.fraInfotrygd(
                    gitt = gittIInfotrygd,
                    fått = fåttIInfotrygd
                )
                false -> SpleisetOverføringer.fraNyLøsning(
                    overføringerINyLøsning = overføringerINyLøsning.getValue(saksnummer!!),
                    saksnummerIdentitetsnummerMapping = saksnummerIdentitetsnummerMapping,
                    periode = periode
                )
            }
            sistOverførtIInfotrygd != null -> SpleisetOverføringer.fraInfotrygd(
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
        sistGitt != null && sistFått != null -> when (sistGitt.isAfter(sistFått)) {
            true -> sistGitt.toLocalDateOslo()
            false -> sistFått.toLocalDateOslo()
        }
        sistGitt != null -> sistGitt.toLocalDateOslo()
        else -> sistFått!!.toLocalDateOslo()
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
            )
            },
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
            )
            }
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
                kilder = it.kilder
            )
            },
            fått = overføringerINyLøsning.fått.filter { it.periode.overlapperMedMinstEnDag(periode) }.map { SpleisetOverføringFått(
                gjennomført = it.gjennomført.toLocalDateOslo(),
                gyldigFraOgMed = it.periode.fom,
                gyldigTilOgMed = it.periode.tom,
                fra = Motpart(
                    id = saksnummerIdentitetsnummerMapping.getValue(it.fra)
                ),
                lengde = Duration.ofDays(it.antallDager.toLong()),
                kilder = it.kilder
            )
            }
        )
    }
}

internal data class SpleisetOverføringGitt(
    val gjennomført: LocalDate,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val til: Motpart,
    val lengde: Duration,
    val kilder: Set<Kilde>)

internal fun SpleisetOverføringGitt.periode() = Periode(
    fom = gyldigFraOgMed,
    tom = gyldigTilOgMed
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