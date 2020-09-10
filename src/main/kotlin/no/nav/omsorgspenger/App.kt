package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException

internal val logger = LoggerFactory.getLogger("no.nav.omsorgspenger")

fun main() {
    var env = System.getenv()

    if(env.containsKey("username") && env.containsKey("password")) {
        logger.info("Contains username & password!")
    }

    val user = "/var/run/secrets/nais.io/service_user/username".readFile()
    val pass = "/var/run/secrets/nais.io/service_user/password".readFile()

    if(user != null ) {
        logger.info("Username found: $user")
    }

    if(pass != null) {
        logger.info("Password found")
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

internal fun String.readFile() =
        try {
            File(this).readText(Charsets.UTF_8)
        } catch (err: FileNotFoundException) {
            logger.error("File not found: $this")
        }
