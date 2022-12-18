import java.awt.Point
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

fun main() {
    data class Step(val dx: Int, val dy: Int) {
        fun update(point: Point) {
            point.translate(dx, dy)
        }

        fun dist() = max(abs(dx), abs(dy))
    }

    val left = Step(-1, 0)
    val right = Step(1, 0)
    val up = Step(0, -1)
    val down = Step(0, 1)

    val keyToStep = mapOf(
        "L" to left,
        "R" to right,
        "U" to up,
        "D" to down
    )

    fun Point.calcStepTo(point: Point): Step {
        return Step(point.x - this.x, point.y - this.y)
    }

    fun Step.toUnitStep(): Step {
        return Step(sign(dx.toDouble()).toInt(), sign(dy.toDouble()).toInt())
    }

    fun loadData() = File("src/main/resources/Day09.txt").readLines()

    fun loadSteps(): List<Step> {
        return loadData().flatMap { line ->
            val step = keyToStep[line.split(" ")[0]]!!
            val stepCount = line.split(" ")[1].toInt()
            mutableListOf<Step>().apply { repeat(stepCount) { add(step) } }
        }
    }

    fun part1(): Int {
        val steps = loadSteps()
        val head = Point(0, 0)
        val tail = Point(0, 0)

        val tailLocationSet = mutableSetOf<Point>()
        tailLocationSet.add(Point(tail))

        for (step in steps) {
            step.update(head)

            val stepTailToHead = tail.calcStepTo(head)
            var s = "head = (${head.x}, ${head.y})"

            if (stepTailToHead.dist() > 1) {
                val prevTail = Point(tail)
                stepTailToHead.toUnitStep().update(tail)
                s = "$s, update tail (${prevTail.x}, ${prevTail.y}) -> (${tail.x}, ${tail.y})"

                if (tailLocationSet.add(Point(tail))) {
                    s = "$s, new loc"
                }
            }

            s.log()
        }

        return tailLocationSet.size
    }

    fun part2(): Int {
        val steps = loadSteps()
        val rope: List<Point> = mutableListOf<Point>().apply { repeat(10) { add(Point(0, 0)) } }

        val tailLocationSet = mutableSetOf<Point>()
        tailLocationSet.add(Point(rope.last()))

        for (step in steps) {
            step.update(rope.first())

            rope.windowed(size = 2).forEach { (firstKnot, secondKnot) ->
                val stepBetweenKnots = secondKnot.calcStepTo(firstKnot)
                if (stepBetweenKnots.dist() > 1) {
                    stepBetweenKnots.toUnitStep().update(secondKnot)
                }
                if (secondKnot === rope.last()) {
                    tailLocationSet.add(Point(secondKnot))
                }
            }
        }

        return tailLocationSet.size
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}