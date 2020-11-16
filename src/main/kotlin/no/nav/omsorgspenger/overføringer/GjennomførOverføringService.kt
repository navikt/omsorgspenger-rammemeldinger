package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Saksnummer
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
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<NyOverføring>) : GjennomførtOverføringer {
        return overføringRepository.gjennomførOverføringer(
            fra = fra,
            til = til,
            overføringer = overføringer
        )
    }
}

internal data class GjennomførtOverføringer(
    internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
    internal val berørteSaksnummer: Set<Saksnummer>) {
    internal val alleSaksnummer = gjeldendeOverføringer.saksnummer()
}

internal fun List<NyOverføring>.somAvslått(fra: Saksnummer, til: Saksnummer) =
    fjernOverføringerUtenDager().let { overføringer ->
        mapOf(
            fra to GjeldendeOverføringer(
                gitt = overføringer.map { GjeldendeOverføringGitt(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    til = til
                )}
            ),
            til to GjeldendeOverføringer(
                fått = overføringer.map { GjeldendeOverføringFått(
                    gjennomført = ZonedDateTime.now(),
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Avslått,
                    fra = fra
                )}
            )
        )
    }.let { GjennomførtOverføringer(
        gjeldendeOverføringer = it,
        berørteSaksnummer = setOf(fra, til)
    )}
