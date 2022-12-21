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
        val stopMap = mutableMapOf<Point, MutableSet<Point>>()

        init {
            loadData().forEach { path -> addRockPath(path) }
            stopMap.putAll(rocks.associateWith { mutableSetOf() })
            rocks.forEach { rock ->
                stopMap[rock]?.addAll(rocks.filter { it.canBeStopBy(rock) })
            }
            stopMap.entries.removeIf { it.value.size == 3 }
        }

        fun updateSurfaceParticles(sand: Point) {
            stopMap.filterKeys { sand.canBeStopBy(it) }.values.forEach { stopCandidates -> stopCandidates.add(sand) }
            stopMap.putIfAbsent(sand, mutableSetOf())
            stopMap[sand]!!.addAll(stopMap.keys.filter { particle -> particle.canBeStopBy(sand) })
            stopMap.entries.removeIf { it.value.size == 3 }
        }

        fun Point.canBeStopBy(p: Point): Boolean {
            return p.y == this.y + 1 && p.x in (this.x - 1..this.x + 1)
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

        fun plot(sandSource: Point? = null) {
            val particles = rocks.union(sands)

            val xStart = particles.minOf { it.x }
            val xEnd = particles.maxOf { it.x }
            val yStart = min(particles.minOf { it.y }, sandSource?.y ?: Int.MAX_VALUE)
            val yEnd = if (withBaseLine) baseLine else particles.maxOf { it.y }
            val figure = Array(yEnd - yStart + 1) { Array(xEnd - xStart + 1) { "." } }

            rocks.forEach { figure[it.y - yStart][it.x - xStart] = "#" }
            sands.forEach { figure[it.y - yStart][it.x - xStart] = "o" }
            if (withBaseLine) {
                (xStart..xEnd).forEach { x -> figure[baseLine - yStart][x - xStart] = "#" }
            }
            stopMap.keys.forEach { figure[it.y - yStart][it.x - xStart] = "${stopMap[it]?.size}" }
            sandSource?.also { figure[it.y - yStart][it.x - xStart] = "+" }

            figure.joinToString(separator = "\n", postfix = "\n") { it.joinToString(separator = "") }.log()
        }

        fun dropSandFrom(sand: Point): Boolean {

            while (true) {
                val targetHit = stopMap.keys.filter { it.x == sand.x && it.y > sand.y }.minByOrNull { it.y } ?: if (withBaseLine) {
                    sand.move(sand.x, baseLine - 1)
                    putSand(sand)
                    return true
                } else {
                    return false
                }

                if (sand.y < targetHit.y - 1) {
                    sand.move(targetHit.x, targetHit.y - 1)
                    continue
                }

                if (sand.apply { translate(-1, 1) } !in stopMap) {
                    continue
                }
                sand.translate(1, -1)

                if (sand.apply { translate(1, 1) } !in stopMap) {
                    continue
                }
                sand.translate(-1, -1)

                putSand(sand)
                return true
            }

        }

        fun putSand(sand: Point) {
            sands.add(sand)
            updateSurfaceParticles(sand)
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
            if (round % 5000 == 0) {
                "===== Round #$round =====".log()
                cave.plot(sandSource)
            }
            cave.dropSandFrom(Point(sandSource))

            if (sandSource in cave.sands) {
                break
            }

            round++
        }
        cave.plot(sandSource)

        return cave.sands.size
    }

    val part1 = part1()
    val part2 = part2()
    println("part1 = $part1")
    println("part2 = $part2")
}