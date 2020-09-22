package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.Folketrygdeloven
import no.nav.omsorgspenger.lovverk.Lovhenvisning

object AleneOmOmsorgenForBarnet: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 sjette ledd AleneOmOmsorgenForBarnet"
}

object UtvidetRettForBarnet: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 sjette ledd UtvidetRettForBarnet"
}

object MedlemIFolketrygden: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 2 MedlemIFolketrygden"
}