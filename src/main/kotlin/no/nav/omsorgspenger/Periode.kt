package no.nav.omsorgspenger

import java.time.LocalDate

internal data class Periode(
    internal val fom: LocalDate,
    internal val tom: LocalDate) {
    constructor(iso: String) : this(LocalDate.parse(iso.split("/")[0]), LocalDate.parse(iso.split("/")[1]))

    init {
        require(tom.isAfter(fom) || fom.isEqual(tom)) {"Ugylidg periode. fom=$fom, tom=$tom"}
    }

    override fun toString() = "$fom/$tom"
}

internal fun Collection<LocalDate>.periodiser() : List<Periode> {
    if (isEmpty()) return emptyList()

    if (size == 1) return listOf(Periode(fom = first(), tom = first()))

    val perioder = mutableListOf<Periode>()

    val knekkpunkter = toMutableSet().sorted()

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
                } catch (ignore: Throwable) { null }

            }
        }?.also {
            perioder.add(it)
            forrigeTom = it.tom
        }
        i++
    }

    return perioder
}