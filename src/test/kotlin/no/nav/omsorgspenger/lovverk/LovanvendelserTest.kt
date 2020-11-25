package no.nav.omsorgspenger.lovverk

import no.nav.omsorgspenger.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

internal class LovanvendelserTest {

    @Test
    fun `serialisering og deserialiseringstest`() {

        @Language("JSON")
        val forventetJson = """
        {
            "2019-01-01/2019-03-05": {
                "Min lov (versjon 1) § 1-2 Noe kult, første ledd, andre punktum": ["En to tre § lov", "Samme periode."]
            },
            "2020-02-03/2020-05-07": {
                "Min lov (versjon 1) § 1-3 Noe kult, første ledd, tredje punktum": ["By design"]
            },
            "2021-12-30/2022-02-02": {
                "Min andre lov (versjon 2) § 1-4 Noe kult, første ledd, fjerde punktum": ["Det var som bare.."]
            }
        }
        """.trimIndent()

        val serialized = TestLovanvendelser.somJson()
        JSONAssert.assertEquals(forventetJson, serialized, true)

        val reserialized = Lovanvendelser.fraJson(serialized).somJson()

        JSONAssert.assertEquals(forventetJson, reserialized, true)

    }

    @Test
    fun `kun anvendelser`() {
        assertEquals(mapOf(
            periode1 to listOf("En to tre § lov", "Samme periode."),
            periode2 to listOf("By design"),
            periode3 to listOf("Det var som bare..")
        ), TestLovanvendelser.kunAnvendelser())
    }

    internal companion object {
        private val periode1 = Periode("2019-01-01/2019-03-05")
        private val periode2 = Periode("2020-02-03/2020-05-07")
        private val periode3 = Periode("2021-12-30/2022-02-02")

        private val MinLov = object: Lov {
            override val id = "Min lov (versjon 1)"
        }
        private val MinAndreLov = object: Lov {
            override val id = "Min andre lov (versjon 2)"
        }
        private val Lovhenvisning1 = object: Lovhenvisning {
            override val lov = MinLov
            override val henvisning = "§ 1-2 Noe kult, første ledd, andre punktum"
        }
        private val Lovhenvisning2 = object: Lovhenvisning {
            override val lov = MinLov
            override val henvisning = "§ 1-3 Noe kult, første ledd, tredje punktum"
        }
        private val Lovhenvisning3 = object: Lovhenvisning {
            override val lov = MinAndreLov
            override val henvisning = "§ 1-4 Noe kult, første ledd, fjerde punktum"
        }

        internal val TestLovanvendelser = Lovanvendelser()
            .leggTil(
                periode = periode1,
                lovhenvisning = Lovhenvisning1, anvendelse = "En to tre § lov"
            ).leggTil(
                periode = periode2,
                lovhenvisning = Lovhenvisning2, anvendelse = "By design"
            ).leggTil(
                periode = periode3,
                lovhenvisning = Lovhenvisning3, anvendelse = "Det var som bare.."
            ).leggTil(
                periode = periode1,
                lovhenvisning = Lovhenvisning1, anvendelse = "Samme periode."
            )
    }
}