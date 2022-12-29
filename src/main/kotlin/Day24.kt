import java.awt.Point
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
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
        val w = lines.maxOf { it.length } - 2
        val h = lines.size - 2
        return Triple(blizzards, Pair(P(1, 0), P(w, h + 1)), Pair(w, h))
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
        safeZones: List<MutableMap<Int, MutableMap<Int, P>>>,
        track: List<P>,
        targetP: P,
        bestShot: IntArray,
        cycle: Int
    ): List<P>? {

        if (safeZones.size < track.size) {
            return null
        }

        val currentP = track.last()

        if (track.size + minkowskiDistance(currentP, targetP) > bestShot[0]) {
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
                bestShot[0] = bestTrackSoFar.size - 1   // exclude start P
                "Track with ${bestShot[0]} steps is found.".logi()
                return bestTrackSoFar
            }

            if (safeZones.getOrNull(track.size)?.get(y)?.get(x) == null) {
                return@map null
            }

            val trackTried = greedySearch(
                safeZones = safeZones,
                track = track.toMutableList().apply { add(P(x, y)) },
                targetP = targetP,
                bestShot = bestShot,
                cycle = cycle
            )

            //  Remove safe zones that we tried in current step so that we won't be able to (and don't need to) step into it again in other attempts.
            safeZones.getOrNull(track.size)?.get(y)?.remove(x)

            return@map trackTried
        }
        return possibleTracks.minByOrNull { it?.size ?: Int.MAX_VALUE }
    }

    fun part1(): Int {
        myLogLevel = 1

        val (blizzards, startEndPs, vallySize) = loadBlizzardMap()
        val (startP, endP) = startEndPs
        val (vallyW, vallyH) = vallySize

        val vallyXRange = (1..vallyW)
        val vallyYRange = (1..vallyH)

        val cycle = leastCommonMultiple(vallyW, vallyH)
        val safeZones = mutableListOf<Map<Int, Map<Int, P>>>()

        repeat(times = cycle) {
            val pts = vallyYRange.associateWith { y -> vallyXRange.associateWith { x -> P(x, y) }.toMutableMap() }.toMutableMap()
            pts.putIfAbsent(startP.y, mutableMapOf())
            pts[startP.y]!![startP.x] = startP
            pts.putIfAbsent(endP.y, mutableMapOf())
            pts[endP.y]!![endP.x] = endP

            blizzards.forEach { blizzard -> pts[blizzard.y]?.remove(blizzard.x) }
            safeZones.add(pts)

            blizzards.forEach { blizzard ->
                blizzard.march()
                if (blizzard.dir.isHorizontal()) {
                    when (blizzard.x) {
                        0 -> blizzard.x = vallyW
                        vallyW + 1 -> blizzard.x = 1
                    }
                } else {
                    when (blizzard.y) {
                        0 -> blizzard.y = vallyH
                        vallyH + 1 -> blizzard.y = 1
                    }
                }
            }
        }

        var track: List<P>?
        var repeat = max(1, ceil(minkowskiDistance(startP, endP).toDouble() / cycle).toInt())
        do {
            val repeatedMutableSafeZones = (1..repeat).flatMap { safeZones.map { sz -> sz.mapValues { it.value.toMutableMap() }.toMutableMap() } }

            "Try with at most ${repeatedMutableSafeZones.size} steps.".logi()

            track = greedySearch(
                safeZones = repeatedMutableSafeZones,
                track = listOf(startP),
                targetP = endP,
                bestShot = intArrayOf(Int.MAX_VALUE),
                cycle = cycle
            )

            if (track == null) {
                "Track not found within ${repeatedMutableSafeZones.size} steps.".logi()
            }

            repeat++
        } while (track == null)

        "Best track found: $track".logi()

        return track.size - 1
    }

    fun part2(): Int {
        myLogLevel = 2

        val (blizzards, startEndPs, vallySize) = loadBlizzardMap()
        val (startP, endP) = startEndPs
        val (vallyW, vallyH) = vallySize

        val vallyXRange = (1..vallyW)
        val vallyYRange = (1..vallyH)

        val cycle = leastCommonMultiple(vallyW, vallyH)
        val safeZones = mutableListOf<Map<Int, Map<Int, P>>>()

        repeat(times = cycle) {
            val pts = vallyYRange.associateWith { y -> vallyXRange.associateWith { x -> P(x, y) }.toMutableMap() }.toMutableMap()
            pts.putIfAbsent(startP.y, mutableMapOf())
            pts[startP.y]!![startP.x] = startP
            pts.putIfAbsent(endP.y, mutableMapOf())
            pts[endP.y]!![endP.x] = endP

            blizzards.forEach { blizzard -> pts[blizzard.y]?.remove(blizzard.x) }
            safeZones.add(pts)

            blizzards.forEach { blizzard ->
                blizzard.march()
                if (blizzard.dir.isHorizontal()) {
                    when (blizzard.x) {
                        0 -> blizzard.x = vallyW
                        vallyW + 1 -> blizzard.x = 1
                    }
                } else {
                    when (blizzard.y) {
                        0 -> blizzard.y = vallyH
                        vallyH + 1 -> blizzard.y = 1
                    }
                }
            }
        }

        val track: MutableList<P> = mutableListOf()
        val stepCounts = mutableListOf<Int>()
        listOf(
            Pair(startP, endP),
            Pair(endP, startP),
            Pair(startP, endP)
        ).forEach { (fromP, toP) ->
            var repeat = max(1, ceil(minkowskiDistance(startP, endP).toDouble() / cycle).toInt())
            do {
                val repeatedMutableSafeZones = (1..repeat)
                    .flatMap { safeZones.map { sz -> sz.mapValues { it.value.toMutableMap() }.toMutableMap() } }
                    .toMutableList()
                    .apply {
                        val passedBlizzard = track.size % cycle
                        addAll(take(passedBlizzard))
                        subList(0, passedBlizzard).clear()
                    }

                "Try with at most ${repeatedMutableSafeZones.size} steps.".logd()

                val oneWayTrack = greedySearch(
                    safeZones = repeatedMutableSafeZones,
                    track = listOf(fromP),
                    targetP = toP,
                    bestShot = intArrayOf(Int.MAX_VALUE),
                    cycle = cycle
                )

                if (oneWayTrack == null) {
                    "Track not found within ${repeatedMutableSafeZones.size} steps.".logd()
                } else {
                    val excludeStartPTrack = oneWayTrack.subList(1, oneWayTrack.size)
                    "Best track heading to $toP has been found: $excludeStartPTrack".logi()
                    track.addAll(excludeStartPTrack)
                    stepCounts.add(excludeStartPTrack.size)
                }

                repeat++
            } while (oneWayTrack == null)
        }

        "Best track found: $track".logi()

        "Sum of the steps: ${stepCounts.joinToString(" + ")} = ${track.size}".logi()

        return track.size
    }

    measureTimeMillis {
        val part1 = part1()
        val part2 = part2()
        println("part1 = $part1")
        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}