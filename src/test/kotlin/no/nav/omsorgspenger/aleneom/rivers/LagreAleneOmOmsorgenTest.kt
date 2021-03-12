package no.nav.omsorgspenger.aleneom.rivers

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.AleneOmOmsorgenBehov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.*
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgen
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class LagreAleneOmOmsorgenTest(
    dataSource: DataSource) {
    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate()
    ).build()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `lagre alene om omsorgen`() {
        val identitetsnummer = IdentitetsnummerGenerator.identitetsnummer()
        val saksnummer = "OP123456"
        val barn1 = IdentitetsnummerGenerator.identitetsnummer()
        val barn2 = IdentitetsnummerGenerator.identitetsnummer()

        val (id, behovssekvens) = Behovssekvens(
            id = ULID().nextULID(),
            correlationId = "${UUID.randomUUID()}",
            behov = arrayOf(AleneOmOmsorgenBehov(
                identitetsnummer = identitetsnummer,
                mottaksdato = LocalDate.parse("2020-12-15"),
                barn = listOf(AleneOmOmsorgenBehov.Barn(
                    identitetsnummer = barn2,
                    fødselsdato = LocalDate.parse("2009-12-12")
                ), AleneOmOmsorgenBehov.Barn(
                    identitetsnummer = barn1,
                    fødselsdato = LocalDate.parse("2003-04-12")
                ))
            ))
        ).keyValue

        rapid.sendTestMessage(behovssekvens)
        rapid.mockHentOmsorgspengerSaksnummer(identitetsnummer, saksnummer)
        val jsonMessage = rapid.sisteMeldingSomJsonMessage().also { it.interestedIn("@løsninger.AleneOmOmsorgen.løst") }
        assertNotNull(jsonMessage["@løsninger.AleneOmOmsorgen.løst"].asText().let {ZonedDateTime.parse(it)})

        val nå = ZonedDateTime.now()
        val lagretAleneOmOmsorgen = applicationContext.aleneOmOmsorgenRepository.hent(saksnummer).map {it.copy(registrert = nå)}
        assertThat(lagretAleneOmOmsorgen).hasSameElementsAs(setOf(
            AleneOmOmsorgen(behovssekvensId = id, registrert = nå, periode = Periode("2020-12-15/2021-12-31"), regstrertIForbindelseMed = "AleneOmOmsorgen", barn = AleneOmOmsorgen.Barn(identitetsnummer = barn1, fødselsdato = LocalDate.parse("2003-04-12"))),
            AleneOmOmsorgen(behovssekvensId = id, registrert = nå, periode = Periode("2020-12-15/2027-12-31"), regstrertIForbindelseMed = "AleneOmOmsorgen", barn = AleneOmOmsorgen.Barn(identitetsnummer = barn2, fødselsdato = LocalDate.parse("2009-12-12")))
        ))
    }


    private companion object {

        private fun TestRapid.mockHentOmsorgspengerSaksnummer(identitetsnummer: Identitetsnummer, saksnummer: Saksnummer) =
            sendTestMessage(sisteMeldingSomJsonMessage().leggTilLøsningPåHentOmsorgspengerSaksnummer(identitetsnummer, saksnummer).toJson())

        private fun JsonMessage.leggTilLøsningPåHentOmsorgspengerSaksnummer(
            identitetsnummer: Identitetsnummer, saksnummer: Saksnummer) = leggTilLøsning(
            behov = HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
            løsning = mapOf(
                "saksnummer" to mapOf(
                    identitetsnummer to saksnummer
                )
            )
        )
    }
}