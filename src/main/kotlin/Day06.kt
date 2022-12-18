import java.io.File

fun main() {
    fun loadData() = File("src/main/resources/Day06.txt").readLines()

    fun part1(): Int {
        val window = 4
        loadData()[0].windowed(window).forEachIndexed { idx, chars ->
            if (chars.toSet().size == window) {
                return idx + window
            }
        }
        return 0
    }

    fun part2(): Int {
        val window = 14
        loadData()[0].windowed(window).forEachIndexed { idx, chars ->
            if (chars.toSet().size == window) {
                return idx + window
            }
        }
        return 0
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}