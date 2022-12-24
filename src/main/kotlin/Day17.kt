import java.io.File
import kotlin.math.max

fun main() {

    class Rock(vararg yxPairs: Int) {
        val pts: Map<Int, Set<Int>>
        val width: Int
        val height: Int

        init {
            val ml = mutableMapOf<Int, MutableSet<Int>>()
            yxPairs.toList().chunked(2).forEach { (y, x) ->
                ml.putIfAbsent(y, mutableSetOf())
                ml[y]!!.add(x)
            }
            pts = ml.toMap()
            width = ml.values.flatten().max() - ml.values.flatten().min() + 1
            height = ml.keys.max() - ml.keys.min() + 1
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
        val rockMap = mutableMapOf<Long, MutableSet<Int>>()

        fun canFitInWith(rock: Rock, y: Long, x: Int): Boolean {
            if (x < 0 || (x + rock.width) > width || y < 0) {
                return false
            }
            if (rockMap[y] == null) {
                return true
            }

            return !rock.pts.any { (rockY, rockXs) -> rockMap[rockY + y]?.any { (it - x) in rockXs } == true }
        }

        fun addRock(rock: Rock, y: Long, x: Int) {
            rock.pts.forEach { (rockY, rockXs) ->
                val depth = y + rockY
                rockMap.putIfAbsent(depth, mutableSetOf())
                rockMap[depth]!!.addAll(rockXs.map { it + x })
            }
        }

        fun dropLayers(fromY: Long) {
            rockMap.entries.removeIf { it.key < fromY }
        }
    }

    fun loadData() = File("src/main/resources/Day17_test.txt").readLines()

    fun loadWindDx() = loadData()[0].map {
        when {
            it.toString() == "<" -> -1
            it.toString() == ">" -> 1
            else -> throw IllegalArgumentException("???")
        }
    }

    fun tetris(rockCount: Long, logLevel: Int = 2): Long {
        myLogLevel = logLevel

        val winds = loadWindDx()
        val chamber = Chamber()

        var windIdx = 0
        var chamberTop = 0L

        (0L until rockCount).forEach { rockIdx ->
            val rock = rocks[(rockIdx % rocks.size).toInt()]

            var rockY = 3L + chamberTop
            var rockX = 2

            while (true) {
                val windDx = winds[windIdx++ % winds.size]

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

    val part1 = tetris(2022, logLevel = 2)
//    val part2 = tetris(1_000_000_000_000L, logLevel = 2)
    println("part1 = $part1")
//    println("part2 = $part2")
}