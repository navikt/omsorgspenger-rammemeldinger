package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.Personopplysninger
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.Enhetstype
import no.nav.omsorgspenger.personopplysninger.Navn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate

internal class OpphøreOverføringerMeldingTest {

    @Test
    fun `Ola opphører overføringene til kari - Olas melding`() {
        val melding = GittDagerOpphørt(
            til = kari,
            fraOgMed = fraOgMed
        )

        val meldingsbestilling = Meldingsbestilling(
            behovssekvensId = "01F1FH9YJVD3BCE3Y1244TDFFE",
            aktørId = ola.aktørId,
            saksnummer = "OLA",
            måBesvaresPerBrev = false,
            melding = melding
        )

        @Language("JSON")
        val forventetJson = """
        {
            "dokumentbestillingId": "01F1FH9YJVD3BCE3Y1244TDFFE-1234",
            "distribuere": false,
            "dokumentMal": "OVERFORE_GITT_DAGER_OPPHORT",
            "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
            "saksnummer": "OLA",
            "aktørId": "1234",
            "ytelseType": "OMSORGSPENGER",
            "dokumentdata": {
                "fraOgMed": "2021-03-21",
                "til": {
                    "navn": {
                        "mellomnavn": "Olasson",
                        "etternavn": "Nordmann",
                        "fornavn": "Kari"
                    },
                    "fødselsdato": "2001-04-21"
                }
            },
            "eksternReferanse": "01F1FH9YJVD3BCE3Y1244TDFFE"
        }
        """.trimIndent()

        meldingsbestilling.keyValue.second.jsonEquals(forventetJson)

    }

    @Test
    fun `Ola opphører overføringene til kari - Karis melding`() {
        val melding = MottattDagerOpphørt(
            fra = ola,
            fraOgMed = fraOgMed
        )

        val meldingsbestilling = Meldingsbestilling(
            behovssekvensId = "01F1FHA8YS28KBXP1ZY98FTVPW",
            aktørId = kari.aktørId,
            saksnummer = "KARI",
            måBesvaresPerBrev = false,
            melding = melding
        )

        @Language("JSON")
        val forventetJson = """
        {
            "dokumentbestillingId": "01F1FHA8YS28KBXP1ZY98FTVPW-5678",
            "distribuere": false,
            "dokumentMal": "OVERFORE_MOTTATT_DAGER_OPPHORT",
            "avsenderApplikasjon": "OMSORGSPENGER_RAMMEMELDINGER",
            "saksnummer": "KARI",
            "aktørId": "5678",
            "ytelseType": "OMSORGSPENGER",
            "dokumentdata": {
                "fraOgMed": "2021-03-21",
                "fra": {
                    "navn": {
                        "etternavn": "Nordmann",
                        "fornavn": "Ola"
                    },
                    "fødselsdato": "2000-05-21"
                }
            },
            "eksternReferanse": "01F1FHA8YS28KBXP1ZY98FTVPW"
        }
        """.trimIndent()

        meldingsbestilling.keyValue.second.jsonEquals(forventetJson)

    }

    private companion object {
        val ola = personopplysninger(
            navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann"),
            aktørId = "1234",
            fødselsdato = LocalDate.parse("2000-05-21")
        )

        val kari = personopplysninger(
            navn = Navn(fornavn = "Kari", mellomnavn = "Olasson", etternavn = "Nordmann"),
            aktørId = "5678",
            fødselsdato = LocalDate.parse("2001-04-21")
        )

        val fraOgMed = LocalDate.parse("2021-03-21")

        private fun personopplysninger(aktørId: AktørId, navn: Navn, fødselsdato: LocalDate) = Personopplysninger(
            navn = navn,
            aktørId = aktørId,
            fødselsdato = fødselsdato,
            adressebeskyttet = false,
            gjeldendeIdentitetsnummer = aktørId,
            enhet = Enhet(enhetstype = Enhetstype.VANLIG, enhetsnummer = aktørId)
        )
        private fun String.jsonEquals(expected: String) = JSONAssert.assertEquals(expected, this, true)
    }

}