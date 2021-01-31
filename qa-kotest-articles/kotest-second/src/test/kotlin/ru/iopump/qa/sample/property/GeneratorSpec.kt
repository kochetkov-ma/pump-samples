package ru.iopump.qa.sample.property

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.merge
import io.kotest.property.exhaustive.times
import org.slf4j.event.Level

/** For string generator with leading zero */
/*1*/val numberCodepoint: Arb<Codepoint> = Arb.int(0x0030..0x0039)
    .map { Codepoint(it) }

/** For english string generator */
/*2*/val engCodepoint: Arb<Codepoint> = Arb.int('a'.toInt()..'z'.toInt())
    .merge(Arb.int('A'.toInt()..'Z'.toInt()))
    .map { Codepoint(it) }

class GeneratorSpec : FreeSpec() {
    init {
        "/*3*/ random number supported leading zero" {
            Arb.string(10, numberCodepoint).next()
                .also(::println)
        }

        "/*4*/ random english string" {
            Arb.string(10, engCodepoint).orNull(0.5).next()
                .also(::println)
        }

        "/*5*/ random russian mobile number" {
            Arb.stringPattern("+7\\(\\d{3}\\)\\d{3}-\\d{2}-\\d{2}").next()
                .also(::println)
        }

        "/*6*/ exhaustive collection and enum multiply" {
            Exhaustive.ints(1..5).times(Exhaustive.enum<Level>()).values
                .also(::println)
        }

        "/*7*/ exhaustive collection and enum merge" {
            Exhaustive.ints(1..5).merge(Exhaustive.enum<Level>()).values
                .also(::println)
        }
    }
}