package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(System.getenv()).apply {
        OmsorgspengerRammemeldinger(this)
    }.start()
}