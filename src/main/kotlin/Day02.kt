import java.io.File

enum class Result(val score: Int) {
    WIN(6),
    LOSE(0),
    DRAW(3)
}

enum class Shape(val shapeScore: Int) {
    ROCK(1),
    PAPER(2),
    SCISSORS(3);

    fun versus(opponent: Shape) = when {
        this == opponent -> Result.DRAW
        (this.ordinal - opponent.ordinal + 3) % 3 == 1 -> Result.WIN
        else -> Result.LOSE
    }
}

fun main() {

    fun loadData() = File("src/main/resources/Day02.txt").readLines()

    fun transformDataToPairs(data: List<String>): List<Pair<String, String>> {
        return data.map { Pair(it[0].toString(), it[2].toString()) }
    }

    val abcToShape = mapOf(
        "A" to Shape.ROCK,
        "B" to Shape.PAPER,
        "C" to Shape.SCISSORS
    )

    fun part1(): Int {

        val xyzToShape = mapOf(
            "X" to Shape.ROCK,
            "Y" to Shape.PAPER,
            "Z" to Shape.SCISSORS
        )

        val scores = transformDataToPairs(loadData())
            .map { (abc, xyz) -> Pair(abcToShape[abc]!!, xyzToShape[xyz]!!) }
            .map { (elfShape, myShape) -> myShape.shapeScore + myShape.versus(elfShape).score }
        return scores.sum()
    }

    fun part2(): Int {

        val xyzToResult = mapOf(
            "X" to Result.LOSE,
            "Y" to Result.DRAW,
            "Z" to Result.WIN
        )

        val scores = transformDataToPairs(loadData())
            .map { (abc, xyz) ->
                val elfShape = abcToShape[abc]!!
                val myShape = Shape.values().find { it.versus(elfShape) == xyzToResult[xyz] }!!
                return@map Pair(elfShape, myShape)
            }
            .map { (elfShape, myShape) -> myShape.shapeScore + myShape.versus(elfShape).score }
        return scores.sum()
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}