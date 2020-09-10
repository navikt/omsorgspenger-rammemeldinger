package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal val logger = LoggerFactory.getLogger("no.nav.omsorgspenger")

fun main() {
    var env = System.getenv()

    if(env.containsKey("username") && env.containsKey("password")) {
        logger.debug("Contains username & password!")
    }

    RapidApplication.create(env).apply {
        OmsorgspengerRammemeldinger(this)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                logger.info("Startup achieved!")
            }
        })
    }.start()
}