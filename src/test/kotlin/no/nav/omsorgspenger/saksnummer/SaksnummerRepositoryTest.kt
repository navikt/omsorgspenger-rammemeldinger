package no.nav.omsorgspenger.saksnummer

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class SaksnummerRepositoryTest(
    private val dataSource: DataSource
) {
    private val saksnummerRepository = SaksnummerRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `henter siste identitetsnummer for hvert etterspurte saksnummer`() {
        leggInnMapping(Identitetsnummer1, Saksnummer1)
        leggInnMapping(Identitetsnummer2, Saksnummer2)
        leggInnMapping(Identitetsnummer3, Saksnummer1)
        leggInnMapping(Identitetsnummer4, Saksnummer2)

        assertEquals(mapOf(
            Identitetsnummer3 to Saksnummer1,
            Identitetsnummer4 to Saksnummer2
        ), saksnummerRepository.hentSisteMappingFor(setOf(Saksnummer1, Saksnummer2)))
        assertEquals(
            mapOf(Identitetsnummer3 to Saksnummer1),
            saksnummerRepository.hentSisteMappingFor(setOf(Saksnummer1))
        )
    }

    @Test
    fun `henter saksnummer for både tidligere og gjeldende identitetsnummer`() {
        leggInnMapping(Identitetsnummer1, Saksnummer1)
        leggInnMapping(Identitetsnummer2, Saksnummer2)
        leggInnMapping(Identitetsnummer3, Saksnummer1)
        leggInnMapping(Identitetsnummer4, Saksnummer2)

        assertEquals(Saksnummer1, saksnummerRepository.hentSaksnummerFor(Identitetsnummer1))
        assertEquals(Saksnummer2, saksnummerRepository.hentSaksnummerFor(Identitetsnummer2))
        assertEquals(Saksnummer1, saksnummerRepository.hentSaksnummerFor(Identitetsnummer3))
        assertEquals(Saksnummer2, saksnummerRepository.hentSaksnummerFor(Identitetsnummer4))
    }

    @Test
    fun `henter siste saksnummer for et identitetsnummer`() {
        leggInnMapping(Identitetsnummer1, Saksnummer1)
        leggInnMapping(Identitetsnummer1, Saksnummer2)

        assertEquals(Saksnummer2, saksnummerRepository.hentSaksnummerFor(Identitetsnummer1))
    }

    @Test
    fun `returnerer tomt resultat når ingen mappinger matcher`() {
        leggInnMapping(Identitetsnummer1, Saksnummer1)

        assertEquals(emptyMap<Identitetsnummer, Saksnummer>(), saksnummerRepository.hentSisteMappingFor(setOf(Saksnummer2)))
        assertEquals(emptyMap<Identitetsnummer, Saksnummer>(), saksnummerRepository.hentSisteMappingFor(emptySet()))
        assertNull(saksnummerRepository.hentSaksnummerFor("404"))
    }

    private fun leggInnMapping(identitetsnummer: Identitetsnummer, saksnummer: Saksnummer) {
        sessionOf(dataSource).use { session ->
            session.run(queryOf(
                "INSERT INTO saksnummer (identitetsnummer, sak) VALUES (?, ?)",
                identitetsnummer,
                saksnummer
            ).asUpdate)
        }
    }

    private companion object {
        private const val Saksnummer1: Saksnummer = "Sak1"
        private const val Saksnummer2: Saksnummer = "Sak2"
        private const val Identitetsnummer1: Identitetsnummer = "1"
        private const val Identitetsnummer2: Identitetsnummer = "2"
        private const val Identitetsnummer3: Identitetsnummer = "3"
        private const val Identitetsnummer4: Identitetsnummer = "4"
    }
}