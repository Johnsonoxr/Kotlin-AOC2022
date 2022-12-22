import java.awt.Point
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun main() {

    class Range(var start: Int, var end: Int) {
        operator fun contains(p: Int): Boolean {
            return p in start..end
        }

        fun unionWith(range: Range): Boolean {
            if (start > range.end + 1 || end < range.start - 1) {
                return false
            }
            start = min(range.start, start)
            end = max(range.end, end)
            return true
        }

        override fun toString(): String {
            return "[$start ~ $end]"
        }
    }

    class ComposedRange {
        val list = mutableListOf<Range>()

        fun add(range: Range) {
            if (list.any { range.start in it && range.end in it }) {
                return
            }
            list.removeIf { it.start in range && it.end in range }

            val startRange = list.find { range.start in it }
            val endRange = list.find { range.end in it }
            when {
                startRange != null && endRange != null -> {
                    list.remove(endRange)
                    startRange.end = endRange.end
                }

                startRange != null -> startRange.end = range.end
                endRange != null -> endRange.start = range.start
                else -> {
                    list.add(range)
                    list.sortBy { it.start }
                }
            }
        }

        override fun toString(): String {
            return "RangeSet(${list.joinToString(", ")})"
        }
    }

    fun loadData() = File("src/main/resources/Day15.txt").readLines()

    fun loadSensorBeaconPairs(): List<Pair<Point, Point>> {
        return loadData().map { line ->
            val numbers = "[-0-9]+".toRegex().findAll(line).map { it.groupValues[0].toInt() }.toList()
            Pair(Point(numbers[0], numbers[1]), Point(numbers[2], numbers[3]))
        }
    }

    fun calcNoBeaconRangeWithFixY(sensor: Point, distance: Int, y: Int): Range? {
        val dx = distance - abs(y - sensor.y)
        return if (dx >= 0) Range(sensor.x - dx, sensor.x + dx) else null
    }

    fun part1(): Int {
        val sbPairs = loadSensorBeaconPairs()
        val targetY = 2000000

        val beaconCoveringXOnTargetY = ComposedRange()

        sbPairs.forEach { (sensor, beacon) ->
            val distance = abs(sensor.x - beacon.x) + abs(sensor.y - beacon.y)
            val range = calcNoBeaconRangeWithFixY(sensor, distance, targetY) ?: return@forEach
            beaconCoveringXOnTargetY.add(range)
        }

        val deviceXsOnTargetY = sbPairs.flatMap { listOf(it.first, it.second) }.toSet().filter { it.y == targetY }.map { it.x }
        return beaconCoveringXOnTargetY.list.sumOf { range -> (range.end - range.start + 1) - deviceXsOnTargetY.count { x -> x in range } }
    }

    fun part2(): Long {
        val sbPairs = loadSensorBeaconPairs()

        val xStart = 0
        val xEnd = 4000000
        val yStart = 0
        val yEnd = 4000000

        val sensorDistanceMap = sbPairs.associate { (sensor, beacon) -> sensor to abs(sensor.x - beacon.x) + abs(sensor.y - beacon.y) }

        (yStart..yEnd).forEach { beaconY ->
            if (beaconY % 100000 == 0) {
                "Scanning at y = $beaconY...".log()
            }
            val beaconCoveringXOnTargetY = ComposedRange()
            sbPairs.forEach innerForeach@{ (sensor, _) ->
                val distance = sensorDistanceMap[sensor]!!
                val range = calcNoBeaconRangeWithFixY(sensor, distance, beaconY) ?: return@innerForeach
                beaconCoveringXOnTargetY.add(range)
            }
            if (beaconCoveringXOnTargetY.list.size > 1) {
                beaconCoveringXOnTargetY.list.map { it.end + 1 }.find { x -> x in xStart..xEnd }?.also { beaconX ->
                    "Found beacon at ($beaconX, $beaconY)".log()
                    return beaconX.toLong() * 4000000L + beaconY.toLong()
                }
            }
        }

        return 0L
    }

    val part1 = part1()
    val part2 = part2()
    println("part1 = $part1")
    println("part2 = $part2")
}