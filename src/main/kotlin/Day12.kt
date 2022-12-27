import java.awt.Point
import java.io.File
import java.lang.IllegalArgumentException


fun main() {
    class TwoDimArr(val arr: Array<Int?>, val stride: Int) {
        operator fun get(p: Point): Int? {
            return if (p.x < 0 || p.x >= stride || p.y < 0 || p.y >= arr.size / stride) {
                null
            } else {
                arr[p.y * stride + p.x]
            }
        }

        operator fun set(p: Point, v: Int?) {
            arr[p.y * stride + p.x] = v
        }
    }

    fun String.code() = this.toCharArray()[0].code

    val aCode = "a".toCharArray()[0].code

    fun loadData() = File("src/main/resources/Day12.txt").readLines()

    fun loadMap(): Triple<TwoDimArr, Point, Point> {
        var start: Point? = null
        var end: Point? = null
        val heightMapStr = loadData()
        val heightMap: Array<Int?> = heightMapStr.flatMapIndexed { row, line ->
            line.mapIndexed { col, char ->
                when {
                    char.isLowerCase() -> char.code - aCode
                    char.toString() == "S" -> {
                        start = Point(col, row)
                        "a".code() - aCode
                    }

                    char.toString() == "E" -> {
                        end = Point(col, row)
                        "z".code() - aCode
                    }

                    else -> throw IllegalArgumentException("WTF")
                }
            }
        }.toTypedArray()
        return Triple(TwoDimArr(heightMap, heightMapStr.first().length), start!!, end!!)
    }

    val (heightMap, startPoint, endPoint) = loadMap()

    fun Point.flatten() = this.y * heightMap.stride + this.x

    fun run(
        startPoint: Point,
        climbableCriteria: (currentH: Int, nextH: Int) -> Boolean,
        finishCriteria: (pioneers: List<Point>) -> Boolean,
        plotFigure: Boolean = false
    ): Int {

        fun Point.move(dir: Dir): Boolean {
            val height = heightMap[this]!!
            this.translate(dir.dx, dir.dy)
            val nextHeight = heightMap[this]
            return if (nextHeight != null && climbableCriteria.invoke(height, nextHeight)) {
                true
            } else {
                this.translate(-dir.dx, -dir.dy)
                false
            }
        }

        val stepsCountMap = TwoDimArr(Array(heightMap.arr.size) { null }, heightMap.stride)
        stepsCountMap[startPoint] = 0

        val pioneers = mutableListOf(Point(startPoint))
        val newPioneers = mutableSetOf<Point>()

        fun plotSearchingStatus() {
            val pioneerLocations = pioneers.map { it.flatten() }
            val plot = stepsCountMap.arr.toList()
                .mapIndexed { i, step ->
                    when {
                        i == startPoint.flatten() -> "S"
                        i == endPoint.flatten() -> "E"
                        i in pioneerLocations -> "#"
                        step != null -> "o"
                        else -> "."
                    }
                }
                .joinToString("")
                .chunked(stepsCountMap.stride)
                .joinToString(separator = "\n")

            plot.print()
        }

        repeat(times = 1000) { round ->
            println("Round #$round: ${pioneers.joinToString(separator = ", ") { "(${it.x}, ${it.y}, h=${heightMap[it]})" }}")
            if (plotFigure) {
                plotSearchingStatus()
                println()
            }

            pioneers.forEach { pioneer ->
                val currentStepCount = stepsCountMap[pioneer]!!
                Dir.values().forEach { dir ->
                    val tester = Point(pioneer)
                    if (tester.move(dir) && (stepsCountMap[tester] == null || stepsCountMap[tester]!! > currentStepCount)) {
                        newPioneers.add(tester)
                        stepsCountMap[tester] = currentStepCount + 1
                    }
                }
            }
            pioneers.clear()
            pioneers.addAll(newPioneers)
            newPioneers.clear()

            if (finishCriteria.invoke(pioneers)) {
                return round + 1
            }
        }

        return -1
    }

    val part1 = run(
        startPoint = startPoint,
        climbableCriteria = { currentH, nextH -> currentH + 1 >= nextH },
        finishCriteria = {pioneers -> endPoint in pioneers },
        plotFigure = false
    )

    val part2 = run(
        startPoint = endPoint,
        climbableCriteria = {currentH, nextH -> currentH - 1 <= nextH },
        finishCriteria = {pioneers -> pioneers.any { pioneer -> heightMap[pioneer] == 0 } },
        plotFigure = true
    )

    println("part1 = $part1")
    println("part2 = $part2")
}