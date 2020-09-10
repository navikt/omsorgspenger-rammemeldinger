package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

internal val logger = LoggerFactory.getLogger("no.nav.omsorgspenger")

fun main() {
    var env = System.getenv()

    RapidApplication.create(env).apply {
        OmsorgspengerRammemeldinger(this)
    }.start()
}