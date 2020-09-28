package no.nav.omsorgspenger

import java.time.LocalDate

internal data class Periode(
    internal val fom: LocalDate,
    internal val tom: LocalDate) {
    constructor(iso: String) : this(LocalDate.parse(iso.split("/")[0]), LocalDate.parse(iso.split("/")[1]))

    init {
        require(tom.isAfter(fom) || fom.isEqual(tom)) {"Ugylidg periode. fom=$fom, tom=$tom"}
    }

    internal fun inneholder(dato: LocalDate) = dato in fom..tom
    internal fun inneholder(periode: Periode) = inneholder(periode.fom) && inneholder(periode.tom)
    internal fun erKantIKant(periode: Periode) = fom.minusDays(1) == periode.tom || tom.plusDays(1) == periode.fom
    internal fun slåSammen(periode: Periode) : Periode {
        require(erKantIKant(periode)) { "Kan ikke slå sammen $this og $periode da de ikke er kant i kant."}
        return Periode(
            fom = if (fom.isBefore(periode.fom)) fom else periode.fom,
            tom = if (tom.isAfter(periode.tom)) tom else periode.tom
        )
    }
    override fun toString() = "$fom/$tom"
}

private fun MutableCollection<LocalDate>.leggTil(periode: Periode) : MutableCollection<LocalDate> {
    add(periode.fom)
    add(periode.tom)
    return this
}

internal fun Collection<LocalDate>.periodiser(
    overordnetPeriode: Periode
) : List<Periode> {

    val perioder = mutableListOf<Periode>()

    val knekkpunkter = toMutableList()
        .leggTil(overordnetPeriode)
        .filter { overordnetPeriode.inneholder(it) }
        .toSet()
        .sorted()

    if (knekkpunkter.size == 1) {
        return listOf(Periode(
            fom = knekkpunkter.first(),
            tom = knekkpunkter.first()
        ))
    }
    if (knekkpunkter.size == 2) {
        return listOf(Periode(
            fom = knekkpunkter.first(),
            tom = knekkpunkter[1]
        ))
    }

    var i = 0
    var forrigeTom : LocalDate? = null

    while (i < knekkpunkter.size) {
        val erFørste = i == 0
        val erSiste = i == knekkpunkter.size - 1

        when {
            erFørste -> {
                Periode(
                    fom = knekkpunkter[i],
                    tom = knekkpunkter[i+1].minusDays(1)
                )
            }
            erSiste -> {
                Periode(
                    fom = forrigeTom!!.plusDays(1),
                    tom = knekkpunkter[i]
                )
            }
            else -> {
                try {
                    Periode(
                        fom = forrigeTom!!.plusDays(1),
                        tom = knekkpunkter[i].minusDays(1)
                    )
                } catch (ignore: IllegalArgumentException) { null }

            }
        }?.also {
            perioder.add(it)
            forrigeTom = it.tom
        }
        i++
    }

    return perioder
}