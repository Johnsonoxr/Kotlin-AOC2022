import java.awt.Point
import java.io.File
import kotlin.math.max
import kotlin.math.min

fun main() {

    fun loadData() = File("src/main/resources/Day14.txt").readLines()

    class Cave {
        val rocks: Set<Point>
        val sands = mutableSetOf<Point>()

        init {
            val rockMutableSet = mutableSetOf<Point>()
            loadData().forEach { path ->
                path.split(" -> ").map { it.split(",").map { n -> n.toInt() } }.windowed(size = 2) { (rockStartP, rockEndP) ->
                    val rocksInPath = if (rockStartP[0] == rockEndP[0]) {
                        val x = rockStartP[0]
                        val from: Int = min(rockStartP[1], rockEndP[1])
                        val to: Int = max(rockStartP[1], rockEndP[1])
                        (from..to).map { y -> Point(x, y) }
                    } else if (rockStartP[1] == rockEndP[1]) {
                        val y = rockStartP[1]
                        val from: Int = min(rockStartP[0], rockEndP[0])
                        val to: Int = max(rockStartP[0], rockEndP[0])
                        (from..to).map { x -> Point(x, y) }
                    } else {
                        throw IllegalArgumentException("WTF")
                    }
                    rockMutableSet.addAll(rocksInPath)
                }
            }
            rocks = rockMutableSet.toSet()
        }

        fun plot() {
            val xStart = rocks.minOf { it.x }
            val xEnd = rocks.maxOf { it.x }
            val yStart = rocks.minOf { it.y }
            val yEnd = rocks.maxOf { it.y }
            val figure = Array(yEnd - yStart + 1) { Array(xEnd - xStart + 1) { "." } }

            rocks.forEach { figure[it.y - yStart][it.x - xStart] = "#" }
            sands.forEach { figure[it.y - yStart][it.x - xStart] = "o" }

            figure.joinToString(separator = "\n") { it.joinToString(separator = "") }.log()
        }

        fun dropSandFrom(sand: Point) {
            while (true) {
                if (sand.apply { translate(0, 1) }.let { it !in rocks && it !in sands }) {
                    continue
                }
                sand.translate(0, -1)

                if (sand.apply { translate(-1, 1) }.let { it !in rocks && it !in sands }) {
                    continue
                }
                sand.translate(1, -1)

                if (sand.apply { translate(1, 1) }.let { it !in rocks && it !in sands }) {
                    continue
                }
                sand.translate(-1, -1)

                sands.add(Point(sand))
                break
            }
        }
    }

    fun packedCalories(data: List<String>): MutableList<MutableList<Int>> {
        val calories = mutableListOf<MutableList<Int>>()
        data.forEach {
            val cal = it.toIntOrNull()
            if (cal != null) {
                if (calories.isEmpty()) {
                    calories.add(mutableListOf())
                }
                calories.last().add(cal)
            } else {
                calories.add(mutableListOf())
            }
        }
        return calories
    }

    fun part1(): Int {
        val cave = Cave()
        repeat(10) { round ->
            "\nRound #$round".log()
            cave.plot()
            cave.dropSandFrom(Point(500, 0))
        }

        "\nFinal".log()
        cave.plot()
        return 0
    }

    println("part1 = ${part1()}")
//    println("part2 = ${part2()}")
}