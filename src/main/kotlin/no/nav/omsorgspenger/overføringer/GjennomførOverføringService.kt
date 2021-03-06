package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import java.time.ZonedDateTime

internal class GjennomførOverføringService(
    private val overføringRepository: OverføringRepository) {

    /**
     * Mottar referanse til personen som overfører 'fra' og
     * personen som man overfører 'til' samt overføringene som skal gjennomføres.
     *
     * Returnerer et map med alle involverte personene i overføringen.
     * Denne inneholder alltid personId for 'fra' og 'til' - men potensielt også
     * tidligere samboer/ektefelle for en eller begge.
     */

    internal fun gjennomførOverføringer(
        behovssekvensId: BehovssekvensId,
        fra: Saksnummer,
        til: Saksnummer,
        antallDagerØnsketOverført: Int,
        lovanvendelser: Lovanvendelser,
        overføringer: List<NyOverføring>) : GjennomførtOverføringer {
        return overføringRepository.gjennomførOverføringer(
            fra = fra,
            til = til,
            overføringer = overføringer,
            behovssekvensId = behovssekvensId,
            lovanvendelser = lovanvendelser,
            antallDagerØnsketOverført = antallDagerØnsketOverført
        )
    }
}

internal data class GjennomførtOverføringer(
    internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
    internal val berørteSaksnummer: Set<Saksnummer>) {
    internal val alleSaksnummer = gjeldendeOverføringer.saksnummer()
    internal fun kunGjeldendeOverføringerForBerørteParter() = GjennomførtOverføringer(
        gjeldendeOverføringer = gjeldendeOverføringer.filterKeys { it in berørteSaksnummer },
        berørteSaksnummer = berørteSaksnummer
    )
}

internal fun List<NyOverføring>.somAvslått(
    fra: Saksnummer,
    til: Saksnummer,
    antallDagerØnsketOverført: Int) =
    fjernOverføringerUtenDager().let { overføringer ->
        mapOf(
            fra to GjeldendeOverføringer(
                gitt = overføringer.map { GjeldendeOverføringGitt(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    til = til,
                    kilder = setOf(),
                    antallDagerØnsketOverført = antallDagerØnsketOverført
                )}
            ),
            til to GjeldendeOverføringer(
                fått = overføringer.map { GjeldendeOverføringFått(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    fra = fra,
                    kilder = setOf()
                )}
            )
        )
    }.let { GjennomførtOverføringer(
        gjeldendeOverføringer = it,
        berørteSaksnummer = setOf(fra, til)
    )}
