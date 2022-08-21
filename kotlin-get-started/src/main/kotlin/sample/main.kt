package sample

fun main() {

    println("circleFactorial")
    circleFactorial()

    println("\nrecursionFactorial")
    recursionFactorial()
}

fun circleFactorial(target: Int = 5) {
    println("Factorial calculating $target ...")
    var result = 1
    for (index in 1..target step 1) {
        result *= index
        println("Step result is: $result")
    }
    println("Factorial result: $result")
}

fun recursionFactorial(target: Int = 5) {
    println("Factorial calculating $target ...")
    val result = step(target)
    println("Factorial result: $result")
}

fun step(value: Int): Int = (if (value == 1) 1 else value * step(value - 1)).also {
    println("Step result is: $it")
}