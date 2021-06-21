package no.nav.omsorgspenger.infotrygd

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import java.time.Duration

internal class InfotrygdRammeService(
    private val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway) {
    private val cache: Cache<CacheKey, List<InfotrygdRamme>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(200)
        .build()

    private suspend fun hentAlle(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) : List<InfotrygdRamme> {
        val cacheKey = CacheKey(identitetsnummer, periode)
        val infotrygdRammer = cache.getIfPresent(cacheKey)?:omsorgspengerInfotrygdRammevedtakGateway.hent(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        )
        return cache.put(cacheKey, infotrygdRammer).let { infotrygdRammer }
    }

    internal suspend fun hentUtvidetRett(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
        hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdUtvidetRettVedtak>()

    internal suspend fun hentFordelingGir(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
        hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdFordelingGirMelding>()

    internal suspend fun hentMidlertidigAlene(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
        hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdMidlertidigAleneVedtak>()

    internal suspend fun hentOverføringGir(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
            hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdOverføringGirMelding>()

    internal suspend fun hentOverføringFår(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
            hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdOverføringFårMelding>()

    internal suspend fun hentAleneOmOmsorgen(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
            hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdAleneOmOmsorgenMelding>()

    internal suspend fun hentKoronaOverføringGir(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
        hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdKoronaOverføringGirMelding>()

    internal suspend fun hentKoronaOverføringFår(identitetsnummer: Identitetsnummer, periode: Periode, correlationId: CorrelationId) =
        hentAlle(identitetsnummer, periode, correlationId).filterIsInstance<InfotrygdKoronaOverføringFårMelding>()

    private companion object {
        private data class CacheKey(
            val identitetsnummer: Identitetsnummer,
            val periode: Periode
        )
    }
}