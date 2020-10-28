package no.nav.omsorgspenger.extensions

import org.slf4j.LoggerFactory
import java.time.Duration

object AntallDager {
    private val logger = LoggerFactory.getLogger(AntallDager::class.java)
    internal fun Duration.antallDager() : Int {
        if (isNegative) return 0
        val antallDager = toDays().toInt()
        return when (Duration.ofDays(toDays())) {
            this -> antallDager
            else -> logger.warn("Runder opp $this til ${antallDager + 1} dager.")
                .let { antallDager + 1 }
        }
    }
}