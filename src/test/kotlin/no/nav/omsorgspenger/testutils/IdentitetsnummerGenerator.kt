package no.nav.omsorgspenger.testutils

import no.nav.omsorgspenger.Identitetsnummer

internal object IdentitetsnummerGenerator {
    private var teller = 10000000000
    internal fun identitetsnummer(): Identitetsnummer {
        return "%011d".format(teller++)
    }
}