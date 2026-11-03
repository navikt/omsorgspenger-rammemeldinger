package no.nav.omsorgspenger.extensions

import java.time.ZoneId
import java.time.ZonedDateTime

internal val Oslo = ZoneId.of("Europe/Oslo")
internal fun ZonedDateTime.toLocalDateOslo() = withZoneSameInstant(Oslo).toLocalDate()