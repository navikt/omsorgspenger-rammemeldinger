package no.nav.omsorgspenger.overføringer

import de.huxhorn.sulky.ulid.ULID
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenResponse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.OmsorgspengerInfotrygdRammevedtakGateway
import no.nav.omsorgspenger.overføringer.IdentitetsnummerGenerator.identitetsnummer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture

internal object IdentitetsnummerGenerator {
    private var teller = 10000000000
    internal fun identitetsnummer(): Identitetsnummer {
        return "%011d".format(teller++)
    }

}
internal fun TestRapid.løsningOverføreOmsorgsdager() = sisteMelding().somMelding().løsningPå(OverføreOmsorgsdagerLøsningResolver.Instance)
internal fun TestRapid.ventPå(antallMeldinger: Int) = Awaitility.await().atMost(Duration.ofSeconds(1)).until { inspektør.size == antallMeldinger }
internal fun TestRapid.ventPåLøsning(
    behovssekvens: String,
    fra: Identitetsnummer,
    til: Identitetsnummer) {
    sendTestMessage(behovssekvens)
    ventPå(antallMeldinger = 1)
    mockLøsningPåHenteOmsorgspengerSaksnummer(fra = fra, til = til)
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
    mottaksdato: LocalDate = LocalDate.now(),
    barn: List<OverføreOmsorgsdagerBehov.Barn> = emptyList(),
    borINorge: Boolean = true,
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
            borINorge = borINorge,
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
        mottaksdato = mottaksdato
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

internal fun TestAppliationContextBuilder() = ApplicationContext.Builder(
    accessTokenClient = mockk<AccessTokenClient>().also {
        every { it.getAccessToken(any()) }.returns(AccessTokenResponse(accessToken = "foo", expiresIn = 1000, tokenType = "Bearer"))
    },
    kafkaProducer = mockk<KafkaProducer<String, String>>().also {
        every { it.send(any()) }.returns(CompletableFuture.completedFuture(RecordMetadata(
            TopicPartition("foo", 1),
            1L,
            1L,
            System.currentTimeMillis(),
            1L,
            1,
            1
        )))
    },
    omsorgspengerInfotrygdRammevedtakGateway = mockk<OmsorgspengerInfotrygdRammevedtakGateway>().also {
        every { it.hent(any(), any(), any())}.returns(listOf())
        coEvery { it.check() }.returns(Healthy("OmsorgspengerInfotrygdRammevedtakGateway", "Mock helsesjekk OK!"))
    }
)