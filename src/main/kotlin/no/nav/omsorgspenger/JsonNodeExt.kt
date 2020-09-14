package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.JsonNode

internal fun JsonNode.requireText() = require(isTextual) { "Må være tekst." }
internal fun JsonNode.requireInt() = require(isIntegralNumber) { "Må være int." }
internal fun JsonNode.requireArray(predicate: (JsonNode) -> Boolean = { true }) = require(isArray && all { predicate(it) }) { "Må være array." }