import java.io.File
import kotlin.math.max
import kotlin.math.min

fun main() {

    fun loadData() = File("src/main/resources/Day18.txt").readLines()

    fun loadPts() = loadData().map { it.split(",").map { i -> i.toInt() } }

    fun calcSurfaces(pts: List<List<Int>>): Int {
        fun calcConnectionAlongAxis(axis: Int): Int {
            val axisLeft = mutableListOf(0, 1, 2).apply { remove(axis) }

            var connection = 0
            val grouping = pts.groupBy { it[axis] }

            (grouping.keys.min()..grouping.keys.max()).windowed(2).forEach { (v1, v2) ->
                val pts1 = grouping[v1] ?: return@forEach
                val pts2 = grouping[v2] ?: return@forEach

                val pts1Tokens = pts1.map { (it[axisLeft[0]] * 100000 + it[axisLeft[1]]) }
                val pts2Tokens = pts2.map { (it[axisLeft[0]] * 100000 + it[axisLeft[1]]) }

                val tokenIntersected = pts1Tokens.intersect(pts2Tokens.toSet())

                connection += tokenIntersected.size
            }
            return connection
        }

        val xConnection = calcConnectionAlongAxis(0)
        val yConnection = calcConnectionAlongAxis(1)
        val zConnection = calcConnectionAlongAxis(2)

        return pts.size * 6 - 2 * (xConnection + yConnection + zConnection)
    }

    fun fillAirPtsOutside(pts: List<List<Int>>): Pair<List<List<Int>>, List<Int>> {

        val xMax = pts.maxOf { it[0] } + 1
        val xMin = pts.minOf { it[0] } - 1
        val yMax = pts.maxOf { it[1] } + 1
        val yMin = pts.minOf { it[1] } - 1
        val zMax = pts.maxOf { it[2] } + 1
        val zMin = pts.minOf { it[2] } - 1

        val lavaPtsMapAlongX = pts.groupBy { it[0] }

        val airPtsMapAlongX = mutableMapOf<Int, MutableSet<List<Int>>>()

        val nearbyOffsets = listOf(
            listOf(-1, 0, 0),
            listOf(1, 0, 0),
            listOf(0, -1, 0),
            listOf(0, 1, 0),
            listOf(0, 0, -1),
            listOf(0, 0, 1)
        )

        do {
            val airPtsCount = airPtsMapAlongX.values.flatten().count()

            "$airPtsCount air units were filled outside.".logi()

            (xMin..xMax).forEach { x ->
                airPtsMapAlongX.putIfAbsent(x, mutableSetOf())

                val nearbyMinX = max(x - 1, airPtsMapAlongX.keys.min())
                val nearbyMaxX = min(x + 1, airPtsMapAlongX.keys.max())

                val airNearbyPts = (nearbyMinX..nearbyMaxX).map { airPtsMapAlongX[it]!! }.flatten()
                val airPtsInX = airPtsMapAlongX[x]
                val lavaPtsInX = lavaPtsMapAlongX[x]

                (yMin..yMax).forEach { y ->
                    (zMin..zMax).forEach { z ->
                        val pt = listOf(x, y, z)
                        val nearbyPts = nearbyOffsets.map { offsets -> pt.zip(offsets) { a, offset -> a + offset } }
                        if (lavaPtsInX?.contains(pt) != true && (x == xMin || nearbyPts.any { it in airNearbyPts })) {
                            airPtsInX?.add(pt)
                        }
                    }
                }
            }
        } while (airPtsCount != airPtsMapAlongX.values.flatten().count())

        return Pair(airPtsMapAlongX.values.flatten(), listOf(xMax - xMin + 1, yMax - yMin + 1, zMax - zMin + 1))
    }

    fun part1(): Int {
        return calcSurfaces(loadPts())
    }

    fun part2(): Int {
        val lavaPts = loadPts()
        val (airPts, cubeSize) = fillAirPtsOutside(lavaPts)
        val airSurface = calcSurfaces(airPts)
        val (xSize, ySize, zSize) = cubeSize
        "Air cube size = $cubeSize".logi()
        return airSurface - 2 * (xSize * ySize + ySize * zSize + zSize * xSize)
    }

    val part1 = part1()
    val part2 = part2()
    println("part1 = $part1")
    println("part2 = $part2")
}