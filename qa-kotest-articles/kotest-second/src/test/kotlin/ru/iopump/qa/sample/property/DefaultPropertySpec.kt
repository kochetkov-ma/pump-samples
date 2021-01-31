package ru.iopump.qa.sample.property

import com.github.f4b6a3.uuid.UuidCreator
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.UUIDVersion
import io.kotest.matchers.string.shouldBeUUID
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.forAll
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.math.sqrt

class DefaultPropertySpec : FreeSpec() {
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()

    init {
        beforeTest { counter = AtomicInteger() }

        "Basic theorem of arithmetic. Any number can be factorized to list of prime" {
            forAll<Int>(1000) { number ->
                val absNumber = number.absoluteValue
                val primeFactors = absNumber.primeFactors
                println("#$num Source number '$absNumber' = $primeFactors")
                primeFactors.all(Int::isPrime)
            }

            checkAll<Int>(1000) { number ->
                val absNumber = number.absoluteValue
                val primeFactors = absNumber.primeFactors
                println("#$num Source number '$absNumber' = $primeFactors")
                primeFactors.forEach { it.isPrime.shouldBeTrue() }
            }
        }

        "UUIDVersion should be matched with regexp" {
            Exhaustive.enum<UUIDVersion>().checkAll { uuidVersion ->
                uuidVersion.generateUuid().toString()
                    .shouldBeUUID(uuidVersion)
                    .also { println("$num $uuidVersion: $it") }
            }
        }
    }
}

val Int.isPrime get() = toBigInteger().isProbablePrime(1)

val Int.primeFactors: Iterable<Int>
    get() {
        // Array that contains all the prime factors of given number.
        val arr: ArrayList<Int> = arrayListOf()
        var n = this
        if (n == 0) return arr
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
fun UUIDVersion.generateUuid(): UUID =
    when (this) {
        UUIDVersion.ANY -> UUID.randomUUID()
        UUIDVersion.V1 -> UuidCreator.getTimeBased()
        UUIDVersion.V2 -> UuidCreator.getDceSecurity(1, 1)
        UUIDVersion.V3 -> UuidCreator.getNameBasedMd5("666")
        UUIDVersion.V4 -> UuidCreator.getRandomBased()
        UUIDVersion.V5 -> UuidCreator.getNameBasedSha1("666")
    }