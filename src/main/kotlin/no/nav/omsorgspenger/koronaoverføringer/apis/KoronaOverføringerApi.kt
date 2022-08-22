package no.nav.omsorgspenger.koronaoverføringer.apis

import io.ktor.server.routing.*
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.apis.GjeldendeOverføringerAdapter
import no.nav.omsorgspenger.apis.GjeldendeOverføringerApi
import no.nav.omsorgspenger.koronaoverføringer.db.KoronaoverføringRepository
import no.nav.omsorgspenger.overføringer.apis.TilgangsstyringRestClient
import no.nav.omsorgspenger.saksnummer.SaksnummerService

internal fun Route.KoronaOverføringerApi(
    koronaoverføringRepository: KoronaoverføringRepository,
    saksnummerService: SaksnummerService,
    tilgangsstyringRestClient: TilgangsstyringRestClient
) = GjeldendeOverføringerApi(
    path = "/korona-overforinger",
    oppslagBeskrivelse = "hente koronaoverføringer",
    saksnummerService = saksnummerService,
    tilgangsstyringRestClient = tilgangsstyringRestClient,
    gjeldendeOverføringerAdapter = object : GjeldendeOverføringerAdapter {
        override fun hentGjeldendeOverføringer(saksnummer: Saksnummer) =
            koronaoverføringRepository.hentAlleOverføringer(setOf(saksnummer))
    }
)