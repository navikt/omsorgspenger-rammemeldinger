package no.nav.omsorgspenger

internal typealias Environment = Map<String, String>

internal fun Environment.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}
internal fun Environment.hentRequiredEnvSet(key: String) : Set<String> = hentRequiredEnv(key)
    .replace(" ", "")
    .split(",")
    .toSet()