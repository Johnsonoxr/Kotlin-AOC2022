import java.awt.Point
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.math.abs

enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);
}

fun main() {
    class Int2dMap(val arr: MutableList<MutableList<Int>>) : List<List<Int>> by arr {
        fun get(point: Point): Int? {
            return try {
                arr[point.y][point.x]
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        }
    }

    fun String.code() = this.toCharArray()[0].code

    val aCode = "a".toCharArray()[0].code

    fun loadData() = File("src/main/resources/Day12.txt").readLines()

    fun loadHeightMap(): Triple<Int2dMap, Point, Point> {
        var start: Point? = null
        var end: Point? = null
        val arr = loadData().mapIndexed { row, line ->
            line.mapIndexed { col, char ->
                when {
                    char.isLowerCase() -> char.code - aCode
                    char.toString() == "S" -> {
                        start = Point(row, col)
                        "a".code() - aCode
                    }

                    char.toString() == "E" -> {
                        end = Point(row, col)
                        "z".code() - aCode
                    }

                    else -> throw IllegalArgumentException("WTF")
                }
            }.toMutableList()
        }.toMutableList()
        return Triple(Int2dMap(arr), start!!, end!!)
    }

    fun part1(): Int {
        val (heightMap, startPoint, endPoint) = loadHeightMap()

        fun Point.move(dir: Dir): Boolean {
            val height = heightMap.get(this)!!
            this.translate(dir.dx, dir.dy)
            val nextHeight = heightMap.get(this)
            return if (nextHeight != null && abs(nextHeight - height) <= 1) {
                true
            } else {
                this.translate(-dir.dx, -dir.dy)
                false
            }
        }

        val stepsCountMap = Int2dMap(MutableList(heightMap.size) { IntArray(heightMap[it].size) { Int.MAX_VALUE }.toMutableList() })
        stepsCountMap.arr[startPoint.y][startPoint.x] = 0

        val pioneers = mutableListOf(Point(startPoint))
        val newPioneers = mutableSetOf<Point>()

        repeat(100) {
            println("Round#$it: $pioneers")
            pioneers.forEach { pioneer ->
                val currentStepCount = stepsCountMap.get(pioneer)!!
                Dir.values().forEach { dir ->
                    val tester = Point(pioneer)
                    if (tester.move(dir) && stepsCountMap.get(tester)!! > currentStepCount) {
                        newPioneers.add(tester)
                        stepsCountMap.arr[tester.y][tester.x] = currentStepCount + 1
                    }
                }
            }
            pioneers.clear()
            pioneers.addAll(newPioneers)
            newPioneers.clear()

            if (stepsCountMap.get(endPoint)!! < Int.MAX_VALUE) {
                return@repeat
            }
        }

        return 0
    }

    println("part1 = ${part1()}")
//    println("part2 = ${part2()}")
}