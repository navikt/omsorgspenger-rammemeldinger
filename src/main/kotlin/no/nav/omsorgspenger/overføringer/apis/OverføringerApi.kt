package no.nav.omsorgspenger.overføringer.apis

import io.ktor.routing.*
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.apis.GjeldendeOverføringerAdapter
import no.nav.omsorgspenger.apis.GjeldendeOverføringerApi
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService

internal fun Route.OverføringerApi(
    overføringRepository: OverføringRepository,
    saksnummerService: SaksnummerService,
    tilgangsstyringRestClient: TilgangsstyringRestClient) = GjeldendeOverføringerApi(
    path = "/overforinger",
    oppslagBeskrivelse = "hente overføringer",
    saksnummerService = saksnummerService,
    tilgangsstyringRestClient = tilgangsstyringRestClient,
    gjeldendeOverføringerAdapter = object : GjeldendeOverføringerAdapter {
        override fun hentGjeldendeOverføringer(saksnummer: Saksnummer) =
            overføringRepository.hentAktiveOverføringer(setOf(saksnummer))
    }
)