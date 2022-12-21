import java.awt.Point
import java.io.File
import kotlin.math.max
import kotlin.math.min

fun main() {

    fun loadData() = File("src/main/resources/Day14.txt").readLines()

    class Cave(val withBaseLine: Boolean = false) {
        val rocks = mutableSetOf<Point>()
        val sands = mutableSetOf<Point>()
        val baseLine: Int by lazy { rocks.maxOf { it.y } + 2 }

        init {
            loadData().forEach { path -> addRockPath(path) }
        }

        fun addRockPath(path: String) {
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
                rocks.addAll(rocksInPath)
            }
        }

        fun plot() {
            val particles = rocks.union(sands)

            val xStart = particles.minOf { it.x }
            val xEnd = particles.maxOf { it.x }
            val yStart = particles.minOf { it.y }
            val yEnd = if (withBaseLine) baseLine else particles.maxOf { it.y }
            val figure = Array(yEnd - yStart + 1) { Array(xEnd - xStart + 1) { "." } }

            rocks.forEach { figure[it.y - yStart][it.x - xStart] = "#" }
            sands.forEach { figure[it.y - yStart][it.x - xStart] = "o" }
            if (withBaseLine) {
                (xStart..xEnd).forEach { x -> figure[baseLine - yStart][x - xStart] = "#" }
            }

            figure.joinToString(separator = "\n", postfix = "\n") { it.joinToString(separator = "") }.log()
        }

        fun dropSandFrom(sand: Point): Boolean {
            val particles = rocks.union(sands)

            while (true) {
                val targetHit = particles.filter { it.x == sand.x && it.y > sand.y }.minByOrNull { it.y } ?: if (withBaseLine) {
                    sand.move(sand.x, baseLine - 1)
                    sands.add(sand)
                    return true
                } else {
                    return false
                }

                if (sand.y < targetHit.y - 1) {
                    sand.move(targetHit.x, targetHit.y - 1)
                    continue
                }

                if (sand.apply { translate(-1, 1) } !in particles) {
                    continue
                }
                sand.translate(1, -1)

                if (sand.apply { translate(1, 1) } !in particles) {
                    continue
                }
                sand.translate(-1, -1)

                sands.add(Point(sand))
                return true
            }
        }
    }

    fun part1(): Int {
        val cave = Cave()

        var round = 1
        while (true) {
//            "===== Round #$round =====".log()
//            cave.plot()
            if (!cave.dropSandFrom(Point(500, 0))) {
                break
            }
            round++
        }
        cave.plot()

        return cave.sands.size
    }

    fun part2(): Int {
        val cave = Cave(withBaseLine = true)

        val sandSource = Point(500, 0)
        var round = 1
        while (true) {
            "===== Round #$round =====".log()
//            cave.plot()
            cave.dropSandFrom(Point(sandSource))

            if (sandSource in cave.sands) {
                break
            }

            round++
        }

        return cave.sands.size
    }

    println("part1 = ${part1()}")
//    println("part2 = ${part2()}")
}