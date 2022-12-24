import java.io.File

fun main() {

    fun loadData() = File("src/main/resources/Day18.txt").readLines()

    fun loadPts() = loadData().map { it.split(",").map { i -> i.toInt() } }

    fun part1(): Int {

        val pts = loadPts()

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

    val part1 = part1()
//    val part2 = part2(logLevel = 2)
    println("part1 = $part1")
//    println("part2 = $part2")
}