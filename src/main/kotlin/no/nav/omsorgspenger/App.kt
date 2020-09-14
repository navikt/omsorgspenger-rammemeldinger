package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.omsorgspenger.overføringer.StartOverføringAvOmsorgsdager

fun main() {
    RapidApplication.create(System.getenv()).apply {
        StartOverføringAvOmsorgsdager(this)
    }.start()
}