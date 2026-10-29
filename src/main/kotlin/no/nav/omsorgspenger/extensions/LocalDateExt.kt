package no.nav.omsorgspenger.extensions

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal val Oslo = ZoneId.of("Europe/Oslo")
internal fun LocalDate.erFørEllerLik(annen: LocalDate) = isBefore(annen) || isEqual(annen)
internal fun ZonedDateTime.toLocalDateOslo() = withZoneSameInstant(Oslo).toLocalDate()