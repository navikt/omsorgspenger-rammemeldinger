package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.Folketrygdeloven
import no.nav.omsorgspenger.lovverk.Lovhenvisning

// https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_5-5#%C2%A79-5

object RettenGjelderTilOgMedÅretBarnetFyller12: Lovhenvisning{
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-5 tredje ledd første punktum"
}

object RettenGjelderTilOgMedÅretBarnetFyller18: Lovhenvisning{
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-5 tredje ledd andre punktum"
}

object GrunnrettOppTilToBarn: Lovhenvisning{
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 første ledd første punktum"
}

object GrunnrettTreEllerFlerBarn: Lovhenvisning{
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 første ledd andre punktum"
}

object AleneOmOmsorgenForBarnet: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 første ledd tredje punktum"
}

object UtvidetRettForBarnet: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 andre ledd første punktum"
}

object UtvidetRettOgAleneOmOmsorgenForBarnet: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 andre ledd andre punktum"
}


// TODO https://github.com/navikt/omsorgspenger-rammemeldinger/issues/9

object AlleredeForbrukteDager : Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6"
}

object AntallOmsorgsdager : Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6"
}

object FordeltBortOmsorgsdager : Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 femte ledd"
}

object ErMidlertidigAlenerOmOmsorgen : Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 9-6 ErMidlertidigAlenerOmOmsorgen"
}

object JobberINorge: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 2-2"
}

object BorINorge: Lovhenvisning {
    override val lov = Folketrygdeloven
    override val henvisning = "§ 2-1"
}