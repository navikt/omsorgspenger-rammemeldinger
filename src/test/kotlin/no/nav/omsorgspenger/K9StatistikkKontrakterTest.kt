package no.nav.omsorgspenger

import no.nav.k9.statistikk.kontrakter.Behandling
import org.junit.jupiter.api.Test

class K9StatistikkKontrakterTest {
    @Test
    internal fun `json virker`() {
        println(Behandling(behandlingId = "XX").toJson())
    }
}