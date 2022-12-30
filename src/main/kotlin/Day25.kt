import java.io.File
import kotlin.math.pow
import kotlin.system.measureTimeMillis

fun main() {

    val snafu2NumberMap = mapOf(
        "=".toCharArray()[0] to -2,
        "-".toCharArray()[0] to -1,
        "0".toCharArray()[0] to 0,
        "1".toCharArray()[0] to 1,
        "2".toCharArray()[0] to 2
    )

    val number2SnafuMap = snafu2NumberMap.keys.associateBy { snafu2NumberMap[it]!! }

    fun String.snafuToDecimal(): Long {
        return this.reversed().mapIndexed { digit, snafuChar ->
            5.0.pow(digit) * snafu2NumberMap[snafuChar]!!
        }.sum().toLong()
    }

    fun Long.decimalToSnafu(): String {
        var r = this % 5
        var d = this / 5
        if (r > 2) {
            r -= 5
            d += 1
        }
        return if (d > 0) {
            d.decimalToSnafu() + number2SnafuMap[r.toInt()]!!
        } else {
            number2SnafuMap[r.toInt()]!!.toString()
        }
    }

    fun loadData() = File("src/main/resources/Day25.txt").readLines()

    fun part1(): String {
        return loadData().sumOf { it.snafuToDecimal() }.decimalToSnafu()
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}