package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Identitetsnummer
import org.slf4j.Logger

internal object OverføringLogg {
    internal fun Logger.gjennomførtOverføring(fra: Identitetsnummer, til: Identitetsnummer, type: String, meldingsbestillingerSendt: Boolean) = require(name == "tjenestekall").also {
        when (meldingsbestillingerSendt) {
            true -> info("GjennomførtOverføring fra=[$fra], til=[$til], type=[$type], meldingsbestillingerSendt=[ja]")
            false -> warn("GjennomførtOverføring fra=[$fra], til=[$til], type=[$type], meldingsbestillingerSendt=[nei]")
        }
    }
}