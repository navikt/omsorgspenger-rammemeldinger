package no.nav.omsorgspenger.infotrygd

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import org.intellij.lang.annotations.Language

private const val omsorgspengerInfotrygdRammevedtakBasePath = "/omsorgspenger-infotrygd-rammevedtak-mock"

internal fun WireMockServer.omsorgspengerInfotrygdRammevedtakBaseUrl() = baseUrl() + omsorgspengerInfotrygdRammevedtakBasePath
internal fun WireMockServer.mockOmsorgspengerInfotrygdRammevedtakHentRammevedtak(identitetsnummer: Identitetsnummer, periode: Periode, response: String = AlleTypeRammevedtak) {
    val expectedBody = """
        {
            "personIdent": "$identitetsnummer",
            "fom": "${periode.fom}",
            "tom": "${periode.tom}"
        }
        """.trimIndent()
    stubFor(WireMock.post("$omsorgspengerInfotrygdRammevedtakBasePath/rammevedtak")
        .withRequestBody(WireMock.equalToJson(expectedBody))
        .withHeader("Authorization", RegexPattern("^Bearer .+$"))
        .withHeader("X-Correlation-ID", AnythingPattern())
        .withHeader("Content-Type", WireMock.equalTo("application/json"))
        .withHeader("Accept", WireMock.equalTo("application/json"))
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(response))
    )
}
internal fun WireMockServer.mockOmsorgspengerInfotrygdRammevedtakIsReady() : WireMockServer {
    stubFor(WireMock.get("$omsorgspengerInfotrygdRammevedtakBasePath/isready")
        .willReturn(WireMock.aResponse()
            .withStatus(200)
    ))
    return this
}

@Language("JSON")
private val AlleTypeRammevedtak = """
{
  "rammevedtak": {
    "Uidentifisert": [
      {
        "vedtatt": "2020-06-23",
        "kilder": [
          {
            "id": "zoo",
            "type": "Personkort"
          }
        ],
        "gyldigFraOgMed": "2020-10-21",
        "gyldigTilOgMed": "2020-12-21"
      }
    ],
    "UtvidetRett": [
      {
        "vedtatt": "2020-06-21",
        "barn": {
          "id": "01019911111",
          "type": "PersonIdent",
          "fødselsdato": "1999-01-01"
        },
        "gyldigFraOgMed": "2020-06-21",
        "gyldigTilOgMed": "2020-06-21",
        "lengde": "PT480H",
        "kilder": [
          {
            "id": "UTV.RETT/20D/01019911111",
            "type": "Personkort"
          }
        ]
      },
      {
        "vedtatt": "2020-06-11",
        "barn": {
          "id": "2001-01-01",
          "type": "Fødselsdato",
          "fødselsdato": "2001-01-01"
        },
        "gyldigFraOgMed": "2020-01-01",
        "gyldigTilOgMed": "2025-12-31",
        "lengde": "PT240H",
        "kilder": [
          {
            "id": "467035394",
            "type": "Brev"
          },
          {
            "id": "UTV.RETT/10D/01010111111",
            "type": "Personkort"
          }
        ]
      }
    ],
    "MidlertidigAleneOmOmsorgen": [
      {
        "vedtatt": "1998-06-21",
        "kilder": [
          {
            "id": "midl.alene.om/17D",
            "type": "Personkort"
          }
        ],
        "gyldigFraOgMed": "1998-06-25",
        "gyldigTilOgMed": "2001-06-25",
        "lengde": "PT408H"
      }
    ],
    "FordelingGir": [
      {
        "vedtatt": "2018-06-17",
        "kilder": [
          {
            "id": "ford/gir",
            "type": "Personkort"
          }
        ],
        "gyldigFraOgMed": "2017-06-17",
        "gyldigTilOgMed": "2018-06-20",
        "mottaker": {
          "id": "29099011114",
          "type": "PersonIdent",
          "fødselsdato": "1990-09-29"
        },
        "lengde": "P1DT5H45M"
      }
    ],
    "AleneOmOmsorgen": [
      {
        "vedtatt": "2016-06-17",
        "gyldigFraOgMed": "2017-06-17",
        "gyldigTilOgMed": "2018-06-20",
        "barn": {   
          "id": "29099011112",
          "type": "PersonIdent",
          "fødselsdato": "1990-09-29"
        },
        "kilder": []
      },
      {
        "vedtatt": "2016-06-18",
        "gyldigFraOgMed": "2017-06-20",
        "gyldigTilOgMed": "2018-06-25",
        "barn": {   
          "id": "1991-09-29",
          "type": "Fødselsdato",
          "fødselsdato": "1991-09-29"
        },
        "kilder": []
      }
    ],
    "OverføringGir": [
      {
        "vedtatt": "2018-06-17",
        "gyldigFraOgMed": "2017-06-17",
        "gyldigTilOgMed": "2018-06-20",
        "mottaker": {   
          "id": "29099011111",
          "type": "PersonIdent",
          "fødselsdato": "1990-09-29"
        },
        "lengde": "P1DT12H",
        "kilder": []
      }
    ],
    "OverføringFår": [
      {
        "vedtatt": "2018-06-17",
        "gyldigFraOgMed": "2017-06-17",
        "gyldigTilOgMed": "2018-06-20",
        "avsender": {   
          "id": "1990-09-29",
          "type": "Fødselsdato",
          "fødselsdato": "1990-09-29"
        },
        "lengde": "P1D",
        "kilder": []
      }
    ]
  }
}
""".trimIndent()