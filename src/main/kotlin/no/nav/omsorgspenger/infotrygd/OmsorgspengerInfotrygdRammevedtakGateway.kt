package no.nav.omsorgspenger.infotrygd

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate

internal class OmsorgspengerInfotrygdRammevedtakGateway(
    private val accessTokenClient: AccessTokenClient,
    private val hentRammevedtakFraInfotrygdScopes: Set<String>,
    omsorgspengerInfotrygdRammevedtakBaseUrl: URI) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val pingUrl = "$omsorgspengerInfotrygdRammevedtakBaseUrl/isready"
    private val rammevedtakUrl = "$omsorgspengerInfotrygdRammevedtakBaseUrl/rammevedtak"

    internal fun hent(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) : List<InfotrygdRamme> {
        val (_, response, result) = rammevedtakUrl
            .httpPost()
            .header(HttpHeaders.Authorization, authorizationHeader())
            .header(HttpHeaders.Accept, "application/json")
            .header(HttpHeaders.ContentType, "application/json")
            .header(HttpHeaders.XCorrelationId, correlationId)
            .body(
                body = JSONObject().also { root ->
                    root.put("fom", periode.fom.toString())
                    root.put("tom", periode.tom.toString())
                    root.put("personIdent", identitetsnummer)
                }.toString(),
                charset = Charsets.UTF_8
            ).responseString(charset = Charsets.UTF_8)

        val json = result.fold(
            success = { JSONObject(it) },
            failure = {
                throw IllegalStateException("HTTP ${response.statusCode} - ${it.message}")
            }
        )

        val rammevedtak = json.getJSONObject("rammevedtak")

        val utvidetRett = rammevedtak.getArray("UtvidetRett").mapJSONObject().map { InfotrygdUtvidetRettVedtak(
            periode = it.periode(),
            kilder = it.kilder(),
            barnetsFødselsdato = it.barnetsFødselsdato(),
            barnetsIdentitetsnummer = it.barnetsIdentitetsnummer()
        )}

        val fordelingGir = rammevedtak.getArray("FordelingGir").mapJSONObject().map { InfotrygdFordelingGirMelding(
            periode = it.periode(),
            kilder = it.kilder(),
            lengde = it.lengde()
        )}

        val midlertidigAlene = rammevedtak.getArray("MidlertidigAleneOmOmsorgen").mapJSONObject().map { InfotrygdMidlertidigAleneVedtak(
            periode = it.periode(),
            kilder = it.kilder()
        )}

        rammevedtak.getArray("Uidentifisert").also { if (!it.isEmpty) {
            logger.info("Antall Uidentifiserte rammevedtak fra Infotrygd = ${it.length()}")
        }}

        return utvidetRett.plus(fordelingGir).plus(midlertidigAlene)
    }

    private fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(hentRammevedtakFraInfotrygdScopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge("OmsorgspengerInfotrygdRammevedtakGateway", accessTokenCheck(), pingOmsorgspengerInfotrygdRammevetakCheck())

    private fun accessTokenCheck() = kotlin.runCatching {
        accessTokenClient.getAccessToken(hentRammevedtakFraInfotrygdScopes).let {
            (SignedJWT.parse(it.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList()?: emptyList()).contains("access_as_application")
        }}.fold(
            onSuccess = { when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }},
            onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
        )


    private suspend fun pingOmsorgspengerInfotrygdRammevetakCheck() =
        pingUrl.httpGet().awaitStringResponseResult().third.fold(
            success = { Healthy("PingOmsorgspengerInfotrygdRammevetak", "OK: $it") },
            failure = { UnHealthy("PingOmsorgspengerInfotrygdRammevetak", "Feil: ${it.message}") }
        )


    private companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerInfotrygdRammevedtakGateway::class.java)
        private fun JSONObject.getArray(key: String) = when (has(key) && get(key) is JSONArray) {
            true -> getJSONArray(key)
            false -> JSONArray()
        }
        private fun JSONObject.periode() = Periode(
            fom = LocalDate.parse(getString("gyldigFraOgMed")),
            tom = LocalDate.parse(getString("gyldigTilOgMed"))
        )
        private fun JSONObject.lengde() = Duration.parse(getString("lengde"))
        private fun JSONObject.kilder() = getJSONArray("kilder")
            .map { it as JSONObject }
            .map { Kilde(
                id = it.getString("id"),
                type = it.getString("type"))
            }.toSet()
        private fun JSONObject.barnetsFødselsdato() = LocalDate.parse(getJSONObject("barn").getString("fødselsdato"))
        private fun JSONObject.barnetsIdentitetsnummer() : Identitetsnummer? {
            val barn = getJSONObject("barn")
            return when (barn.getString("type") == "PersonIdent") {
                true -> barn.getString("id")
                false -> null
            }
        }
        private fun JSONArray.mapJSONObject() = map { it as JSONObject }
    }
}