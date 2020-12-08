package no.nav.omsorgspenger.koronaoverføringer.formidling

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.Personopplysninger
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.ZonedDateTime

internal class MeldingsbestillingTest {

    @Test
    fun `Innvilget overføring`() {
        val meldingsbestillinger = Formidling.opprettMeldingsbestillinger(
            "bs1",
            personopplysninger = personopplysninger,
            behovet = behovet(
                fra = "id1",
                til = "id2",
                omsorgsdagerÅOverføre = 10
            ),
            behandling = behandling(10)
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.keyValue.second
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.keyValue.second
        @Language("JSON")
        val forventetGitt = """
        {
         "dokumentbestillingId": "bs1-aktør1",
         "distribuere": false,
         "dokumentMal": "KORONA_OVERFORE_GITT_DAGER",
         "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
         "saksnummer": "sak1",
         "aktørId": "aktør1",
         "ytelseType": "OMSORGSPENGER",
         "dokumentdata": {
          "mottaksdato": "2020-12-05",
          "til": {
           "navn": {
            "etternavn": "Nordmann",
            "fornavn": "Kari",
            "mellomnavn": "Johnson"
           },
           "fødselsdato": "1991-02-03"
          },
          "antallDagerØnsketOverført": 10,
          "overføringer": [{
           "gjelderFraOgMed": "2020-01-01",
           "gjelderTilOgMed": "2020-12-31",
           "antallDager": 10
          }]
         },
         "eksternReferanse": "bs1"
        }
        """.trimIndent()
        @Language("JSON")
        val forventetMottatt = """
        {
         "dokumentbestillingId": "bs1-aktør2",
         "distribuere": false,
         "dokumentMal": "KORONA_OVERFORE_MOTTATT_DAGER",
         "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
         "saksnummer": "sak2",
         "aktørId": "aktør2",
         "ytelseType": "OMSORGSPENGER",
         "dokumentdata": {
          "fra": {
           "navn": {
            "etternavn": "Nordmann",
            "fornavn": "Ola"
           },
           "fødselsdato": "1990-09-29"
          },
          "mottaksdato": "2020-12-05",
          "overføringer": [{
           "gjelderFraOgMed": "2020-01-01",
           "gjelderTilOgMed": "2020-12-31",
           "antallDager": 10
          }]
         },
         "eksternReferanse": "bs1"
        }
        """.trimIndent()

        gitt.jsonEquals(forventetGitt)
        mottatt.jsonEquals(forventetMottatt)

    }

    @Test
    fun `Delvis innvilget overføring`() {
        val meldingsbestillinger = Formidling.opprettMeldingsbestillinger(
            "bs2",
            personopplysninger = personopplysninger,
            behovet = behovet(
                fra = "id1",
                til = "id2",
                omsorgsdagerÅOverføre = 10
            ),
            behandling = behandling(5)
        )

        assertThat(meldingsbestillinger).hasSize(2)
        val gitt = meldingsbestillinger.first { it.melding is GittDager }.keyValue.second
        val mottatt = meldingsbestillinger.first { it.melding is MottattDager }.keyValue.second
        @Language("JSON")
        val forventetGitt = """
        {
         "dokumentbestillingId": "bs2-aktør1",
         "distribuere": false,
         "dokumentMal": "KORONA_OVERFORE_GITT_DAGER",
         "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
         "saksnummer": "sak1",
         "aktørId": "aktør1",
         "ytelseType": "OMSORGSPENGER",
         "dokumentdata": {
          "mottaksdato": "2020-12-05",
          "til": {
           "navn": {
            "etternavn": "Nordmann",
            "fornavn": "Kari",
            "mellomnavn": "Johnson"
           },
           "fødselsdato": "1991-02-03"
          },
          "antallDagerØnsketOverført": 10,
          "overføringer": [{
           "gjelderFraOgMed": "2020-01-01",
           "gjelderTilOgMed": "2020-12-31",
           "antallDager": 5
          }]
         },
         "eksternReferanse": "bs2"
        }
        """.trimIndent()
        @Language("JSON")
        val forventetMottatt = """
        {
         "dokumentbestillingId": "bs2-aktør2",
         "distribuere": false,
         "dokumentMal": "KORONA_OVERFORE_MOTTATT_DAGER",
         "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
         "saksnummer": "sak2",
         "aktørId": "aktør2",
         "ytelseType": "OMSORGSPENGER",
         "dokumentdata": {
          "fra": {
           "navn": {
            "etternavn": "Nordmann",
            "fornavn": "Ola"
           },
           "fødselsdato": "1990-09-29"
          },
          "mottaksdato": "2020-12-05",
          "overføringer": [{
           "gjelderFraOgMed": "2020-01-01",
           "gjelderTilOgMed": "2020-12-31",
           "antallDager": 5
          }]
         },
         "eksternReferanse": "bs2"
        }
        """.trimIndent()

        gitt.jsonEquals(forventetGitt)
        mottatt.jsonEquals(forventetMottatt)
    }

    @Test
    fun `Avslått overføring`() {
        val meldingsbestillinger = Formidling.opprettMeldingsbestillinger(
            "bs3",
            personopplysninger = personopplysninger,
            behovet = behovet(
                fra = "id2",
                til = "id1",
                omsorgsdagerÅOverføre = 10
            ),
            behandling = behandling(0)
        )

        assertThat(meldingsbestillinger).hasSize(1)
        val avslag = meldingsbestillinger.first { it.melding is Avslag }.keyValue.second
        @Language("JSON")
        val forventetAvslag = """
        {
         "dokumentbestillingId": "bs3-aktør2",
         "distribuere": false,
         "dokumentMal": "KORONA_OVERFORE_AVSLAG",
         "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
         "saksnummer": "sak1",
         "aktørId": "aktør2",
         "ytelseType": "OMSORGSPENGER",
         "dokumentdata": {
          "mottaksdato": "2020-12-05",
          "til": {
           "navn": {
            "etternavn": "Nordmann",
            "fornavn": "Ola"
           },
           "fødselsdato": "1990-09-29"
          },
          "antallDagerØnsketOverført": 10
         },
         "eksternReferanse": "bs3"
        }
        """.trimIndent()

        avslag.jsonEquals(forventetAvslag)
    }

    @Test
    fun `Overføring til og fra person som er adressebeskyttet`() {
        val meldingsbestillinger = Formidling.opprettMeldingsbestillinger(
            "bs4",
            personopplysninger = personopplysninger,
            behovet = behovet(
                fra = "id1",
                til = "id3",
                omsorgsdagerÅOverføre = 10
            ),
            behandling = behandling(10)
        ).plus(Formidling.opprettMeldingsbestillinger(
            "bs5",
            personopplysninger = personopplysninger,
            behovet = behovet(
                fra = "id3",
                til = "id2",
                omsorgsdagerÅOverføre = 10
            ),
            behandling = behandling(10)
        ))
        assertThat(meldingsbestillinger).isEmpty()
    }

    private companion object {
        private val periode = Periode("2020-01-01/2020-12-31")
        private fun behovet(
            fra: Identitetsnummer,
            til: Identitetsnummer,
            omsorgsdagerÅOverføre: Int
        ) = OverføreKoronaOmsorgsdagerMelding.Behovet(
            fra = fra,
            til = til,
            jobberINorge = true,
            barn = listOf(),
            mottaksdato = LocalDate.parse("2020-12-05"),
            mottatt = ZonedDateTime.now(),
            journalpostIder = setOf(),
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            omsorgsdagerTattUtIÅr = 0,
            periode = periode
        )
        private fun behandling(antallDager: Int) = OverføreKoronaOmsorgsdagerBehandlingMelding.ForVidereBehandling(
            fraSaksnummer = "sak1",
            tilSaksnummer = "sak2",
            overføring = NyOverføring(antallDager = antallDager, periode = periode),
            gjeldendeOverføringer = emptyMap(),
            alleSaksnummerMapping = emptyMap()
        )
        private val personopplysninger = mapOf(
            "id1" to Personopplysninger(
                gjeldendeIdentitetsnummer = "id1",
                fødselsdato = LocalDate.parse("1990-09-29"),
                navn = Personopplysninger.Navn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                ),
                aktørId = "aktør1",
                adressebeskyttet = false
            ),
            "id2" to Personopplysninger(
                gjeldendeIdentitetsnummer = "id2",
                fødselsdato = LocalDate.parse("1991-02-03"),
                navn = Personopplysninger.Navn(
                    fornavn = "Kari",
                    mellomnavn = "Johnson",
                    etternavn = "Nordmann"
                ),
                aktørId = "aktør2",
                adressebeskyttet = false
            ),
            "id3" to Personopplysninger(
                gjeldendeIdentitetsnummer = "id3",
                fødselsdato = LocalDate.now(),
                navn = Personopplysninger.Navn(
                    fornavn = "Svein",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                ),
                aktørId = "aktør3",
                adressebeskyttet = true
            )
        )

        private fun String.jsonEquals(expected: String) = JSONAssert.assertEquals(expected, this, true)
    }
}