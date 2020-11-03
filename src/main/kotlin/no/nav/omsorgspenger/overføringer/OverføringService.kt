package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksreferanse
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class OverføringService(private val infotrygdRammeService: InfotrygdRammeService) {
    /**
     * Mottar referanse til personen som overfører 'fra' og
     * personen som man overfører 'til' samt overføringene som skal gjennomføres.
     *
     * Returnerer et map med alle involverte personene i overføringen.
     * Denne inneholder alltid personId for 'fra' og 'til' - men potensielt også
     * tidligere samboer/ektefelle for en eller begge.
     */
    internal fun gjennomførOverføringer(
        fra: Saksreferanse,
        til: Saksreferanse,
        overføringer: List<Overføring>) : Map<Identitetsnummer, GjeldendeOverføringer> {

        return mapOf(
            fra.identitetsnummer to GjeldendeOverføringer(
                saksnummer = fra.saksnummer,
                gitt = overføringer.gitt(til)
            ),
            til.identitetsnummer to GjeldendeOverføringer(
                saksnummer = til.saksnummer,
                fått = overføringer.fått(fra)
            )
        )
    }

    internal fun hentOverføringer(
            identitetsnummer: Identitetsnummer,
            periode: Periode,
            correlationId: CorrelationId): EGjeldendeOverføringer {
        val gitt = infotrygdRammeService.hentOverføringGir(identitetsnummer, periode, correlationId)
        val fått = infotrygdRammeService.hentOverføringFår(identitetsnummer, periode, correlationId)

        return EGjeldendeOverføringer(
                gitt = gitt.map {
                    EOverføringGitt(
                            gjennomført = it.dato,
                            periode = it.periode,
                            til = AnnenPart(
                                    id = it.annenPart.id,
                                    type = it.annenPart.type,
                                    fødselsdato = it.annenPart.fødselsdato
                            ),
                            lengde = it.lengde
                    )
                },
                fått = fått.map {
                    EOverføringFått(
                            gjennomført = it.dato,
                            periode = it.periode,
                            fra = AnnenPart(
                                    id = it.annenPart.id,
                                    type = it.annenPart.type,
                                    fødselsdato = it.annenPart.fødselsdato
                            ),
                            lengde = it.lengde
                    )
                }
        )
    }
}

internal fun Map<Identitetsnummer, GjeldendeOverføringer>.berørteIdentitetsnummer() : Set<Identitetsnummer> {
    val identitetsnummer = mutableSetOf<Identitetsnummer>()
    forEach { key, value ->
        identitetsnummer.add(key)
        value.fått.forEach { identitetsnummer.add(it.fra.identitetsnummer) }
        value.gitt.forEach { identitetsnummer.add(it.til.identitetsnummer) }
    }
    return identitetsnummer
}