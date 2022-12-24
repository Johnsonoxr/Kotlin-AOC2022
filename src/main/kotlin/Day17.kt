import java.io.File
import kotlin.math.log
import kotlin.math.pow

fun main() {

    class Rock(vararg yxPairs: Int) {
        val pts: Map<Int, Int>
        val width: Int
        val height: Int

        init {
            val ml = mutableMapOf<Int, Int>()
            yxPairs.toList().chunked(2).forEach { (y, x) ->
                ml.putIfAbsent(y, 0)
                ml[y] = ml[y]!! + 2.toDouble().pow(x).toInt()
            }
            pts = ml.toMap()
            width = log(ml.values.max().toDouble(), 2.0).toInt() + 1
            height = ml.keys.max() + 1
        }
    }

    val rocks = listOf(
        Rock(0, 0, 0, 1, 0, 2, 0, 3),
        Rock(0, 1, 1, 0, 1, 1, 1, 2, 2, 1),
        Rock(0, 0, 0, 1, 0, 2, 1, 2, 2, 2),
        Rock(0, 0, 1, 0, 2, 0, 3, 0),
        Rock(0, 0, 0, 1, 1, 0, 1, 1)
    )

    class Chamber {
        val width = 7
        val rockMap = mutableMapOf<Long, Int>()

        fun canFitInWith(rock: Rock, y: Long, x: Int): Boolean {
            if (x < 0 || (x + rock.width) > width || y < 0) {
                return false
            }
            if (rockMap[y] == null) {
                return true
            }

            return rock.pts.all { (rockY, rockX) -> (rockMap[rockY + y] ?: return@all true) and (rockX shl x) == 0 }
        }

        fun addRock(rock: Rock, y: Long, x: Int) {
            rock.pts.forEach { (rockY, rockXs) ->
                val depth = y + rockY
                rockMap.putIfAbsent(depth, 0)
                rockMap[depth] = rockMap[depth]!! or (rockXs shl x)
            }

            if (rockMap.size > 100) {
                clearRedundantLayers()
            }
        }

        fun clearRedundantLayers() {
            val chamberTop = rockMap.keys.maxOrNull() ?: return

            val layerTested = 3
            var layerRemoved = false

            (chamberTop - 10 until chamberTop).reversed().windowed(layerTested).forEach { layerIndices ->
                if (layerRemoved) {
                    return@forEach
                }
                if (layerIndices.map { rockMap[it]!! }.reduce { acc, i -> acc or i } == (2.0.pow(width).toInt()) - 1) {
                    rockMap.entries.removeIf { it.key < layerIndices.last() - 1 }
                    layerRemoved = true
                    "Remove layer data below ${layerIndices.last() - 1}".logd()
                }
            }
        }
    }

    fun loadData() = File("src/main/resources/Day17.txt").readLines()

    fun loadWindDx() = loadData()[0].map {
        when {
            it.toString() == "<" -> -1
            it.toString() == ">" -> 1
            else -> throw IllegalArgumentException("???")
        }
    }

    fun part1(logLevel: Int = 2): Long {
        myLogLevel = logLevel

        val winds = loadWindDx()
        val chamber = Chamber()

        var windIdx = 0
        var chamberTop = 0L

        repeat(times = 2022) { rockIdx ->
            val rock = rocks[rockIdx % rocks.size]

            var rockY = 3L + chamberTop
            var rockX = 2

            while (true) {
                val windDx = winds[windIdx]
                windIdx = (windIdx + 1) % winds.size

                if (chamber.canFitInWith(rock, rockY, rockX + windDx)) {
                    rockX += windDx
                    "Wind blow [$windDx]".logv()
                } else {
                    "Wind blow [$windDx] in vain".logv()
                }

                val gravityDy = -1
                if (chamber.canFitInWith(rock, rockY + gravityDy, rockX)) {
                    rockY += gravityDy
                    "Gravity pull [$gravityDy]".logv()
                } else {
                    chamber.addRock(rock, rockY, rockX)
                    chamberTop = (chamber.rockMap.keys.maxOrNull() ?: -1L) + 1L
                    "Rock #$rockIdx lands on ($rockX, $rockY), chamber's top is $chamberTop".logd()
                    break
                }

                "Rock position = ($rockX, $rockY)".logv()
            }

            "".logv()
        }

        "Done: top of chamber is $chamberTop".logi()
        return chamberTop
    }

    fun part2(logLevel: Int = 2): Long {
        myLogLevel = logLevel

        val winds = loadWindDx()
        val chamber = Chamber()

        val totalRockCount = 1000000000000

        var windIdx = 0
        var chamberTop = 0L

        var patternMap: MutableMap<Rock, MutableSet<Int>>? = null
        var rockIdxInMonitor = 0L
        var recordedChamberTop = 0L
        var skipped = false

        var rockIdx = 0L
        while (rockIdx < totalRockCount) {
            val rock = rocks[(rockIdx % rocks.size).toInt()]

            if (rockIdx > 100000 && !skipped) {
                if (patternMap == null) {
                    "Start monitoring pattern at Rock #$rockIdx".logi()
                    patternMap = mutableMapOf()
                    rockIdxInMonitor = rockIdx
                    recordedChamberTop = chamberTop
                }

                patternMap.putIfAbsent(rock, mutableSetOf())

                val windIndicesOfRock = patternMap[rock]!!
                if (windIdx in windIndicesOfRock) {
                    val cycleOfRocks = rockIdx - rockIdxInMonitor
                    val cycleOfYOffset = chamberTop - recordedChamberTop

                    val skipCycle = (totalRockCount - rockIdx) / cycleOfRocks
                    val skippedRocks = skipCycle * cycleOfRocks
                    val skippedOffset = skipCycle * cycleOfYOffset
                    skipped = true

                    rockIdx += skippedRocks
                    chamberTop += skippedOffset
                    chamber.rockMap.putAll(chamber.rockMap.map { it.key + skippedOffset to it.value })

                    ("Pattern found within cycle of $cycleOfRocks rocks along with y-offset $cycleOfYOffset," +
                            " skip $skippedRocks rocks and $skippedOffset y-offset. ${totalRockCount - rockIdx} rocks left.").logi()
                } else {
                    windIndicesOfRock.add(windIdx)
                }
            }

            var rockY = 3L + chamberTop
            var rockX = 2

            while (true) {
                val windDx = winds[windIdx]
                windIdx = (windIdx + 1) % winds.size

                if (chamber.canFitInWith(rock, rockY, rockX + windDx)) {
                    rockX += windDx
                    "Wind blow [$windDx]".logv()
                } else {
                    "Wind blow [$windDx] in vain".logv()
                }

                val gravityDy = -1
                if (chamber.canFitInWith(rock, rockY + gravityDy, rockX)) {
                    rockY += gravityDy
                    "Gravity pull [$gravityDy]".logv()
                } else {
                    chamber.addRock(rock, rockY, rockX)
                    chamberTop = (chamber.rockMap.keys.maxOrNull() ?: -1L) + 1L
                    "Rock #$rockIdx lands on ($rockX, $rockY), chamber's top is $chamberTop".logd()
                    break
                }

                "Rock position = ($rockX, $rockY)".logv()
            }

            rockIdx++

            "".logv()
        }

        "Done: top of chamber is $chamberTop".logi()
        return chamberTop
    }

    val part1 = part1(logLevel = 2)
    val part2 = part2(logLevel = 2)
    println("part1 = $part1")
    println("part2 = $part2")
}