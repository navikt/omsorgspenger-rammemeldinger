package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.GjennomførtOverføringer
import java.time.ZonedDateTime

internal data class NyOverføring(
    val antallDager: Int,
    val periode: Periode) {
    internal val skalGjennomføres = antallDager > 0
    internal fun somAvslått(
        behovssekvensId: BehovssekvensId,
        fra: Saksnummer,
        til: Saksnummer,
        antallDagerØnsketOverført: Int
    ) = GjennomførtOverføringer(
        berørteSaksnummer = setOf(fra, til),
        gjeldendeOverføringer = mapOf(
            fra to GjeldendeOverføringer(
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = antallDager,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    til = til,
                    kilder = setOf(Kilde.internKilde(
                        behovssekvensId = behovssekvensId,
                        type = "KoronaOverføring"
                    )),
                    antallDagerØnsketOverført = antallDagerØnsketOverført
                ))
            ),
            til to GjeldendeOverføringer(
                fått = listOf(GjeldendeOverføringFått(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = antallDager,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    fra = fra,
                    kilder = setOf(Kilde.internKilde(
                        behovssekvensId = behovssekvensId,
                        type = "KoronaOverføring"
                    ))
                ))
            )
        )
    )
}