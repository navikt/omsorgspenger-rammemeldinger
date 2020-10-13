package no.nav.omsorgspenger.infotrygd

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import java.time.Duration

internal class InfotrygdRammeService(
    private val omsorgspengerInfotrygdRammevedtakGateway: OmsorgspengerInfotrygdRammevedtakGateway) {
    private val cache: Cache<CacheKey, List<InfotrygdRamme>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(200)
        .build()

    private fun hentAlle(identitetsnummer: Identitetsnummer, periode: Periode) : List<InfotrygdRamme> {
        val cacheKey = CacheKey(identitetsnummer, periode)
        val infotrygdRammer = cache.getIfPresent(cacheKey)?:omsorgspengerInfotrygdRammevedtakGateway.hent(
            identitetsnummer = identitetsnummer,
            periode = periode
        )
        return cache.put(cacheKey, infotrygdRammer).let { infotrygdRammer }
    }

    internal fun hentUtvidetRett(identitetsnummer: Identitetsnummer, periode: Periode) =
        hentAlle(identitetsnummer, periode).filterIsInstance<InfotrygdUtvidetRettVedtak>()

    internal fun hentFordelingGir(identitetsnummer: Identitetsnummer, periode: Periode) =
        hentAlle(identitetsnummer, periode).filterIsInstance<InfotrygdFordelingGirMelding>()

    internal fun hentMidlertidigAlene(identitetsnummer: Identitetsnummer, periode: Periode) =
        hentAlle(identitetsnummer, periode).filterIsInstance<InfotrygdMidlertidigAleneVedtak>()

    private companion object {
        private data class CacheKey(
            val identitetsnummer: Identitetsnummer,
            val periode: Periode
        )
    }
}