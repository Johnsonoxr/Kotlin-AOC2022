import java.awt.Point
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun main() {

    fun loadData() = File("src/main/resources/Day15_test.txt").readLines()

    fun loadSensorBeaconPairs(): List<Pair<Point, Point>> {
        return loadData().map { line ->
            val numbers = "[-0-9]+".toRegex().findAll(line).map { it.groupValues[0].toInt() }.toList()
            Pair(Point(numbers[0], numbers[1]), Point(numbers[2], numbers[3]))
        }
    }

    fun createMask(center: Point, distance: Int): List<Point> {
        val lst = mutableListOf<Point>()
        (-distance..distance).forEach { dy ->
            val maxDx = distance - abs(dy)
            lst.addAll((-maxDx..maxDx).map { dx -> Point(center.x + dx, center.y + dy) })
        }
        "center = $center, distance = $distance, coverage = ${lst.size}".log()
        return lst
    }

    fun part1(): Int {
        val sbPairs = loadSensorBeaconPairs()

        val noBeaconLocations = mutableSetOf<Point>()

        fun plot() {
            val xMin = min(sbPairs.minOf { min(it.first.x, it.second.x) }, noBeaconLocations.minOfOrNull { it.x } ?: Int.MAX_VALUE)
            val xMax = max(sbPairs.maxOf { max(it.first.x, it.second.x) }, noBeaconLocations.maxOfOrNull { it.x } ?: Int.MIN_VALUE)
            val yMin = min(sbPairs.minOf { min(it.first.y, it.second.y) }, noBeaconLocations.minOfOrNull { it.y } ?: Int.MAX_VALUE)
            val yMax = max(sbPairs.maxOf { max(it.first.y, it.second.y) }, noBeaconLocations.maxOfOrNull { it.y } ?: Int.MIN_VALUE)
            val figure = Array(yMax - yMin + 1) { Array(xMax - xMin + 1) { "." } }
            noBeaconLocations.forEach { figure[it.y - yMin][it.x - xMin] = "#" }
            sbPairs.forEach { (sensor, beacon) ->
                figure[sensor.y - yMin][sensor.x - xMin] = "S"
                figure[beacon.y - yMin][beacon.x - xMin] = "B"
            }
            figure.joinToString(separator = "\n", postfix = "\n") { it.joinToString(separator = "") }.log()
        }

        plot()

        sbPairs.forEach { (sensor, beacon) ->
            val distance = abs(sensor.x - beacon.x) + abs(sensor.y - beacon.y)
            noBeaconLocations.addAll(createMask(sensor, distance))
            plot()
        }

        return noBeaconLocations.count { it.y == 10 && it !in sbPairs.flatMap { pair -> listOf(pair.first, pair.second) } }
    }

    val part1 = part1()
//    val part2 = part2()
    println("part1 = $part1")
//    println("part2 = $part2")
}