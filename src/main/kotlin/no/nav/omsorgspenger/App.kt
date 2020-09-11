package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    var env = System.getenv()

    RapidApplication.create(env).apply {
        OmsorgspengerRammemeldinger(this)
    }.start()

}