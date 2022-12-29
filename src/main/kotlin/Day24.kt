import java.awt.Point
import java.io.File
import kotlin.math.abs
import kotlin.system.measureTimeMillis

fun main() {

    open class P(x: Int, y: Int) : Point(x, y) {
        override fun toString() = "P($x, $y)"

        fun translate(dir: Dir) {
            translate(dir.dx, dir.dy)
        }
    }

    class Blizzard(x: Int, y: Int, val dir: Dir) : P(x, y) {
        fun march() {
            translate(dir)
        }
    }

    fun loadData() = File("src/main/resources/Day24.txt").readLines()

    /**
     * @return (blizzards, (startP, endP), (valleyWidth, valleyHeight)
     */
    fun loadBlizzardMap(): Triple<List<Blizzard>, Pair<P, P>, Pair<Int, Int>> {
        val lines = loadData()
        val blizzards = mutableListOf<Blizzard>()
        val regex = "[<v^>]".toRegex()
        lines.forEachIndexed { y, line ->
            blizzards.addAll(regex.findAll(line).map { match -> Blizzard(match.range.first, y, Dir.values().first { it.plot() == match.value }) })
        }
        val w = lines.maxOf { it.length }
        val h = lines.size
        return Triple(blizzards, Pair(P(1, 0), P(w - 2, h - 1)), Pair(w, h))
    }

    fun leastCommonMultiple(a: Int, b: Int): Int {
        fun gcd(a: Int, b: Int): Int {
            return if (a % b == 0) b else gcd(b, a % b)
        }
        return a * b / gcd(a, b)
    }

    fun minkowskiDistance(p1: P, p2: P): Int {
        return abs(p1.x - p2.x) + abs(p1.y - p2.y)
    }

    val searchOffsets = listOf(
        P(1, 0),
        P(0, 1),
        P(0, 0),
        P(-1, 0),
        P(0, -1),
    )

    fun greedySearch(
        safeZones: List<Map<Int, Map<Int, P>>>,
        track: List<P>,
        targetP: P,
        bestShot: IntArray,
        cycle: Int
    ): List<P>? {

        val currentP = track.lastOrNull() ?: P(1, 0)

        if (track.size + minkowskiDistance(currentP, targetP) >= bestShot[0]) {
            //  Not a chance to find better track.
            "Early stop".logv()
            return null
        }

        val pOfPreviousCycles = track.slice(((track.size % cycle) until track.size) step cycle)

        "Current tracks (${track.size}): $track".logv()

        if (currentP in pOfPreviousCycles) {
            //  Went back to the same place where you have been in previous cycles.
            "Same place in previous cycles".logv()
            return null
        }

        val possibleTracks = searchOffsets.map { offset ->
            val x = currentP.x + offset.x
            val y = currentP.y + offset.y

            if (x == targetP.x && y == targetP.y) {
                val bestTrackSoFar = track.toMutableList().apply { add(targetP) }
                bestShot[0] = bestTrackSoFar.size
                "Track found with ${bestTrackSoFar.size} steps. Track = $bestTrackSoFar.".logi()
                return bestTrackSoFar
            }

            if (safeZones[track.size % safeZones.size][y]?.get(x) == null && !(x == 1 && y == 0)) {
                return@map null
            }

            return@map greedySearch(
                safeZones = safeZones,
                track = track.toMutableList().apply { add(P(x, y)) },
                targetP = targetP,
                bestShot = bestShot,
                cycle = cycle
            )
        }
        return possibleTracks.minByOrNull { it?.size ?: Int.MAX_VALUE }
    }

    fun part1() {
        myLogLevel = 1

        val (blizzards, startEndPs, vallySize) = loadBlizzardMap()
        val (startP, endP) = startEndPs
        val (mapW, mapH) = vallySize

        val vallyXRange = (1..mapW - 2)
        val vallyYRange = (1..mapH - 2)
        val vallyW = mapW - 2
        val vallyH = mapH - 2

        val cycle = leastCommonMultiple(vallyW, vallyH)
        val safeZones = mutableListOf<Map<Int, Map<Int, P>>>()

        repeat(times = cycle) {

            blizzards.forEach { blizzard ->
                blizzard.march()
                if (blizzard.dir.isHorizontal() && blizzard.x !in vallyXRange) {
                    blizzard.x = (blizzard.x + vallyW) % vallyW
                } else if (blizzard.dir.isVertical() && blizzard.y !in vallyYRange) {
                    blizzard.y = (blizzard.y + vallyH) % vallyH
                }
            }

            val pts = vallyYRange.associateWith { y -> vallyXRange.associateWith { x -> P(x, y) }.toMutableMap() }.toMutableMap()
            pts.putIfAbsent(startP.y, mutableMapOf())
            pts[startP.y]!![startP.x] = startP
            pts.putIfAbsent(endP.y, mutableMapOf())
            pts[endP.y]!![endP.x] = endP

            blizzards.forEach { blizzard -> pts[blizzard.y]?.remove(blizzard.x) }
            safeZones.add(pts)
        }

        val track = greedySearch(
            safeZones = safeZones,
            track = emptyList(),
            targetP = endP,
            bestShot = intArrayOf(Int.MAX_VALUE),
            cycle = cycle
        )

        "Track found: $track".logi()
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}