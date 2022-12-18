import java.io.File

fun main() {

    fun loadData() = File("src/main/resources/Day10.txt").readLines()

    fun loadRegisterXs(): List<Int> {
        val additions = loadData().map { line ->
            when (line) {
                "noop" -> null
                else -> line.substring(startIndex = 5).toInt()
            }
        }
        val destructedAdditions = additions.map { addition ->
            when (addition) {
                null -> listOf(0)
                else -> listOf(0, addition)
            }
        }.flatten().toMutableList()

        val registerXs = mutableListOf<Int>()
        registerXs.add(1)
        for (addition in destructedAdditions) {
            registerXs.add(addition + registerXs.last())
        }
        registerXs.removeLast()

        return registerXs
    }

    fun part1(): Int {
        val registerXs = loadRegisterXs()

        val signalIndices = (19 until registerXs.size step 40)

        return signalIndices.sumOf { idx ->
            val signalStrength = idx + 1
            registerXs[idx] * signalStrength
        }
    }

    fun part2() {
        val registerXs = loadRegisterXs()

        registerXs.chunked(size = 40).forEach { xs ->
            val line = xs.mapIndexed { i, x ->
                when (i) {
                    in (x - 1)..(x + 1) -> "#"
                    else -> "."
                }
            }.joinToString(separator = "")
            println(line)
        }
    }

    println("part1 = ${part1()}")
    part2()
}