package no.nav.omsorgspenger.overføringer

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.Oslo
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator.identitetsnummer
import no.nav.omsorgspenger.testutils.sisteMelding
import no.nav.omsorgspenger.testutils.ventPå
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*

internal fun TestRapid.løsningOverføreOmsorgsdager() = sisteMelding().somMelding().løsningPå(OverføreOmsorgsdagerLøsningResolver.Instance)
internal fun TestRapid.ventPåLøsning(
    behovssekvens: String,
    fra: Identitetsnummer,
    til: Identitetsnummer,
    barn: Set<Identitetsnummer>,
    borsammen: Boolean) {
    sendTestMessage(behovssekvens)
    ventPå(antallMeldinger = 1)
    mockLøsningPåHenteOmsorgspengerSaksnummerOchVurderRelasjoner(fra = fra, til = til, barn = barn, borSammen = borsammen)
    ventPå(antallMeldinger = 2)
    mockLøsningPåHentePersonopplysninger(fra = fra, til = til)
    ventPå(antallMeldinger = 3)
}

internal fun behovssekvensOverføreOmsorgsdager(
    id: String = ULID().nextValue().toString(),
    overføringFra: Identitetsnummer = identitetsnummer(),
    overføringTil: Identitetsnummer = identitetsnummer(),
    omsorgsdagerTattUtIÅr: Int = 0,
    omsorgsdagerÅOverføre: Int = 10,
    mottaksdato: LocalDate = LocalDate.parse("2020-12-15"),
    barn: List<OverføreOmsorgsdagerBehov.Barn> = emptyList(),
    jobberINorge: Boolean = true,
    relasjon: OverføreOmsorgsdagerBehov.Relasjon = OverføreOmsorgsdagerBehov.Relasjon.NåværendeEktefelle,
    harBoddSammenMinstEttÅr: Boolean? = null,
    kilde: OverføreOmsorgsdagerBehov.Kilde = OverføreOmsorgsdagerBehov.Kilde.Digital
) = Behovssekvens(
    id = id,
    correlationId = UUID.randomUUID().toString(),
    behov = arrayOf(OverføreOmsorgsdagerBehov(
        fra = OverføreOmsorgsdagerBehov.OverførerFra(
            identitetsnummer = overføringFra,
            jobberINorge = jobberINorge
        ),
        til = OverføreOmsorgsdagerBehov.OverførerTil(
            identitetsnummer = overføringTil,
            relasjon = relasjon,
            harBoddSammenMinstEttÅr = harBoddSammenMinstEttÅr
        ),
        omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
        omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
        barn = barn,
        kilde = kilde,
        journalpostIder = listOf(),
        mottatt = ZonedDateTime.of(mottaksdato, LocalTime.now(Oslo), Oslo)
    ))
).keyValue

internal fun overføreOmsorgsdagerBarn(
    identitetsnummer: Identitetsnummer = identitetsnummer(),
    fødselsdato: LocalDate = LocalDate.now().minusYears(5),
    utvidetRett: Boolean = false,
    aleneOmOmsorgen: Boolean = false
) = OverføreOmsorgsdagerBehov.Barn(
    identitetsnummer = identitetsnummer,
    utvidetRett = utvidetRett,
    aleneOmOmsorgen = aleneOmOmsorgen,
    fødselsdato = fødselsdato
)

internal fun Map<String, OverføreOmsorgsdagerLøsning.Overføringer>.assertOverføringer(
    fra: Identitetsnummer,
    til: Identitetsnummer,
    forventedeOverføringer: Map<Periode, Int>) {

    // Skal inneholde overføring for både den som har gitt og den som har fått
    assertTrue(containsKey(fra))
    assertTrue(containsKey(til))

    // Ser at den som har gitt dager har alle forventede overføringer som 'gitt'
    assertEquals(forventedeOverføringer.size, getValue(fra).gitt.size)
    forventedeOverføringer.forEach { (periode, antallDager) ->
        assertNotNull(getValue(fra).gitt.firstOrNull { it.antallDager == antallDager && periode == Periode(fom = it.gjelderFraOgMed, tom = it.gjelderTilOgMed) })
    }
    assertEquals(0, getValue(fra).fått.size)

    // Ser at den som har fått dager har alle forventede oveføringer som 'fått'
    assertEquals(0, getValue(til).gitt.size)
    assertEquals(forventedeOverføringer.size, getValue(til).fått.size)
    forventedeOverføringer.forEach { (periode, antallDager) ->
        assertNotNull(getValue(til).fått.firstOrNull { it.antallDager == antallDager && periode == Periode(fom = it.gjelderFraOgMed, tom = it.gjelderTilOgMed) })
    }
}