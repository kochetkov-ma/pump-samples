package ru.iopump.qa.sample.property

import com.github.f4b6a3.uuid.UuidCreator
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.UUIDVersion
import io.kotest.matchers.string.shouldBeUUID
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.withEdgecases
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.andNull
import io.kotest.property.exhaustive.enum
import io.kotest.property.forAll
import java.util.*
import kotlin.math.sqrt

class PropertySpec : FreeSpec() {
    init {

        "Basic theorem of arithmetic. Any number can be factorized to list of prime" {
            /*1*/Arb.int(2..Int.MAX_VALUE).withEdgecases(2, Int.MAX_VALUE).forAll(1000) { number ->
            val primeFactors = number.primeFactors
            println("#${attempts()} Source number '$number' = $primeFactors")
            /*2*/primeFactors.all(Int::isPrime) && primeFactors.reduce(Int::times) == number
        }

            /*3*/Arb.int(2..Int.MAX_VALUE).checkAll(1000) { number ->
            val primeFactors = number.primeFactors
            println("#${attempts()} Source number '$number' = $primeFactors")
            /*4*/primeFactors.onEach { it.isPrime.shouldBeTrue() }.reduce(Int::times) shouldBe number
        }
        }

        "UUIDVersion should be matched with regexp" {
            /*1*/Exhaustive.enum<UUIDVersion>().andNull().checkAll { uuidVersion ->
            /*2*/uuidVersion.generateUuid().toString()
            /*3*/.shouldBeUUID(uuidVersion ?: UUIDVersion.ANY)
            .also { println("${attempts()} $uuidVersion: $it") }
        }
        }
    }
}

val Int.isPrime get() = toBigInteger().isProbablePrime(1)

val Int.primeFactors: Collection<Int>
    get() {
        // Array that contains all the prime factors of given number.
        val arr: ArrayList<Int> = arrayListOf()
        var n = this
        if (n in (0..1)) throw IllegalArgumentException("Factorized number must be grater then 1")
        // At first check for divisibility by 2. add it in arr till it is divisible
        while (n % 2 == 0) {
            arr.add(2)
            n /= 2
        }

        val squareRoot = sqrt(n.toDouble()).toInt()

        // Run loop from 3 to square root of n. Check for divisibility by i. Add i in arr till it is divisible by i.
        for (i in 3..squareRoot step 2) {
            while (n % i == 0) {
                arr.add(i)
                n /= i
            }
        }

        // If n is a prime number greater than 2.
        if (n > 2) {
            arr.add(n)
        }

        return arr
    }

/** Using [uuid-creator](https://github.com/f4b6a3/uuid-creator) */
fun UUIDVersion?.generateUuid(): UUID =
    when (this) {
        null -> UUID.randomUUID()
        UUIDVersion.ANY -> UUID.randomUUID()
        UUIDVersion.V1 -> UuidCreator.getTimeBased()
        UUIDVersion.V2 -> UuidCreator.getDceSecurity(1, 1)
        UUIDVersion.V3 -> UuidCreator.getNameBasedMd5("666")
        UUIDVersion.V4 -> UuidCreator.getRandomBased()
        UUIDVersion.V5 -> UuidCreator.getNameBasedSha1("666")
    }