package reflection

fun main() {
    val l644: Double = 1.00 + 4.00 + 1.84 + 5.48 + 1.58
    val l854: Double = 1.00 + 1.60 + 3.14 + 6.22 + 4.39 + 6.40

    println("Путь до 644: $l644")
    println("Путь до 854: $l854")

    println("854 / 644:" + (l854 / l644))
    println("644 / 854:" + (l644 / l854))
}