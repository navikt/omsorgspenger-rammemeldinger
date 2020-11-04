package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Saksnummer

internal class OverføringService {
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
        overføringer: List<NyOverføring>) : Map<Saksnummer, GjeldendeOverføringer> {

        // TODO: Kalle på repository

        return overføringer.somGjeldendeOverføringer(
            fra = fra,
            til = til
        )
    }
}

internal fun List<NyOverføring>.somGjeldendeOverføringer(fra: Saksnummer, til: Saksnummer) =
    fjernOverføringerUtenDager().let { overføringer ->
        mapOf(
            fra to GjeldendeOverføringer(
                gitt = overføringer.map { GjeldendeOverføringGitt(
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    til = til
                )}
            ),
            til to GjeldendeOverføringer(
                fått = overføringer.map { GjeldendeOverføringFått(
                    antallDager = it.antallDager,
                    periode = it.periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    fra = fra
                )}
            )
        )
    }
