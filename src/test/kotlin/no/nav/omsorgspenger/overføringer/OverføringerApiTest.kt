package no.nav.omsorgspenger.overføringer

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Duration
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class OverføringerApiTest(
    private val dataSource: DataSource) {

    private val overføringServiceMock = mockk<OverføringService>().also {
        every { it.hentSpleisetOverføringer(any(), any(), any()) }.returns(SpleisetOverføringer(
            gitt = listOf(SpleisetOverføringGitt(
                gjennomført = LocalDate.parse("2018-01-01"),
                gyldigFraOgMed = LocalDate.parse("2019-01-01"),
                gyldigTilOgMed = LocalDate.parse("2020-01-01"),
                til = Motpart(id = "29098811111", type = "Identitetsnummer"),
                lengde = Duration.ofDays(5),
                kilder = setOf(Kilde(id = "1", type = "OmsorgspengerRammemeldinger"))
            )),
            fått = listOf(SpleisetOverføringFått(
                gjennomført = LocalDate.parse("2019-01-01"),
                gyldigFraOgMed = LocalDate.parse("2020-01-01"),
                gyldigTilOgMed = LocalDate.parse("2021-01-01"),
                fra = Motpart(id = "29098811111", type = "Identitetsnummer"),
                lengde = Duration.ofDays(3),
                kilder = setOf(Kilde(id = "2", type = "OmsorgspengerRammemeldinger"))

            ))
        ))
    }
    @Test
    fun `hent overføringer`() {

        withTestApplication({
            omsorgspengerRammemeldinger(TestApplicationContextBuilder(dataSource.cleanAndMigrate()).also { builder ->
                builder.overføringService = overføringServiceMock
            }.build())
        }) {
            handleRequest(HttpMethod.Post, "/hentOverfoeringer") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                @Language("JSON")
                val body = """
                    {
                        "identitetsnummer": "12345678900",
                        "fom": "2020-01-01",
                        "tom": "2020-12-31"
                    }
                """.trimIndent()
                setBody(body)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                @Language("JSON")
                val forventetResponse = """
                {
                    "gitt": [{
                        "gjennomført": "2018-01-01",
                        "gyldigFraOgMed": "2019-01-01",
                        "gyldigTilOgMed": "2020-01-01",
                        "til": {
                            "id": "29098811111",
                            "type": "Identitetsnummer"
                        },
                        "lengde": "PT120H",
                        "kilder": [{
                            "id": "1",
                            "type": "OmsorgspengerRammemeldinger"
                        }]
                    }],
                    "fått": [{
                        "gjennomført": "2019-01-01",
                        "gyldigFraOgMed": "2020-01-01",
                        "gyldigTilOgMed": "2021-01-01",
                        "fra": {
                            "id": "29098811111",
                            "type": "Identitetsnummer"
                        },
                        "lengde": "PT72H",
                        "kilder": [{
                            "id": "2",
                            "type": "OmsorgspengerRammemeldinger"
                        }]
                    }]
                }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }
}