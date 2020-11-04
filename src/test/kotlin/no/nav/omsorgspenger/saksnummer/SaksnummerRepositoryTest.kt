package no.nav.omsorgspenger.saksnummer

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class SaksnummerRepositoryTest(
    dataSource: DataSource){
    private val saksnummerRepository = SaksnummerRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `Test håndtering av saksnummer & identitetsnummer`() {
        val førsteMapping = mapOf(
            Identitetsnummer1 to Saksnummer1,
            Identitetsnummer2 to Saksnummer2
        )
        val saksnummer = førsteMapping.values.toSet()

        // Mapping kan settes inn x antall ganger med samme resultat
        for (x in 1..5) {
            saksnummerRepository.lagreMapping(førsteMapping)
            assertEquals(førsteMapping,saksnummerRepository.hentSisteMappingFor(saksnummer))
        }

        // Om en av sakene får et nytt identitesnummer er det kun den mappingen som blir brukt
        saksnummerRepository.lagreMapping(mapOf(Identitetsnummer3 to Saksnummer1))
        assertEquals(mapOf(
            Identitetsnummer3 to Saksnummer1,
            Identitetsnummer2 to Saksnummer2
        ), saksnummerRepository.hentSisteMappingFor(saksnummer))

        // Om også det andre saksnummeret får et nytt identitetsnummer er det kun de siste mappingene som hentes
        saksnummerRepository.lagreMapping(mapOf(Identitetsnummer4 to Saksnummer2))
        assertEquals(mapOf(
            Identitetsnummer3 to Saksnummer1,
            Identitetsnummer4 to Saksnummer2
        ), saksnummerRepository.hentSisteMappingFor(saksnummer))
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