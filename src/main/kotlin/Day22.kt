import java.io.File
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

fun main() {

    data class P(val x: Int, val y: Int) {
        override fun toString(): String {
            return "P($x, $y)"
        }

        fun offset(dir: Dir): P = offset(dir.dx, dir.dy)

        fun offset(dx: Int, dy: Int): P = P(this.x + dx, this.y + dy)
    }

    data class Rect(var x1: Int, var y1: Int, var x2: Int, var y2: Int) {

        val w: Int
            get() = this.x2 - this.x1 + 1

        val h: Int
            get() = this.y2 - this.y1 + 1

        operator fun contains(p: P): Boolean {
            return p.x in x1..x2 && p.y in y1..y2
        }

        operator fun contains(rect: Rect): Boolean {
            return rect.x1 >= x1 && rect.x2 <= x2 && rect.y1 >= y1 && rect.y2 <= y2
        }

        fun offset(dir: Dir): Rect {
            return Rect(x1 + dir.dx, y1 + dir.dy, x2 + dir.dx, y2 + dir.dy)
        }

        override fun toString(): String {
            return "Rect($x1, $y1 ~ $x2, $y2)"
        }
    }

    data class Move(val step: Int, val turn: Turn?)

    class GroveMap(val w: Int, val h: Int, val faces: List<Rect>, val walls: Set<P>)

    abstract class Wrapper {
        abstract fun wrap(p: P, dir: Dir): Pair<P, Dir>
    }

    fun loadData() = File("src/main/resources/Day22.txt").readLines()

    fun loadGroveMap(): Triple<GroveMap, List<Move>, P> {

        val lines = loadData()
        val splitter = lines.indexOfFirst { it.isEmpty() }

        val groveWidth = lines.take(splitter).maxOf { it.length }
        val groveLines = lines.take(splitter).map { it + " ".repeat(groveWidth - it.length) }

        val faceRegex = "[.|#]+".toRegex()
        val faces = mutableListOf<Rect>()

        val walls = mutableSetOf<P>()
        val wallRegex = "#".toRegex()

        val edgeLength = sqrt(groveLines.joinToString(separator = "").count { !it.isWhitespace() } / 6.0).toInt()

        var startP: P? = null

        groveLines.forEachIndexed { idx, line ->
            val y = idx + 1

            val faceFractions = faceRegex.findAll(line).toList()
                .filter { it.value.isNotEmpty() }
                .flatMap { it.range.chunked(edgeLength) }
                .map { Rect(it.first() + 1, y, it.last() + 1, y) }

            faceFractions.forEach { fraction ->
                val connectedFace = faces.firstOrNull { it.h < edgeLength && fraction.x1 == it.x1 && fraction.x2 == it.x2 && it.y2 + 1 == y }
                if (connectedFace != null) {
                    connectedFace.y2 = y
                } else {
                    faces.add(fraction)
                }
            }

            walls.addAll(wallRegex.findAll(line).map { P(it.range.first + 1, y) })

            if (startP == null) {
                startP = P(line.indexOfFirst { it.toString() == "." } + 1, y)
            }
        }

        val moves = "[0-9]+|L|R".toRegex().findAll(lines.last())
            .map { it.value }.toList().chunked(2)
            .map {
                when (it.size) {
                    2 -> Move(it[0].toInt(), Turn.valueOf(it[1]))
                    else -> Move(it[0].toInt(), null)
                }
            }

        return Triple(GroveMap(groveWidth, groveLines.size, faces, walls), moves, startP!!)
    }

    fun wrap(groveMap: GroveMap, p: P, dir: Dir): P {
        var validP = P(p.x, p.y)
        while (true) {
            val nextP = validP.offset(dir.reverseDir())
            if (groveMap.faces.none { nextP in it }) {
                return validP
            }
            validP = nextP
        }
    }

    class TransportWrapper(groveMap: GroveMap) : Wrapper() {

        val edgePairs: Map<Boolean, List<Pair<Rect, Rect>>>

        init {
            val verticalEdges = groveMap.faces.flatMap { listOf(Rect(it.x1, it.y1, it.x1, it.y2), Rect(it.x2, it.y1, it.x2, it.y2)) }
            val unconnectedVerticalEdges = verticalEdges
                .filter { edge -> groveMap.faces.none { edge.offset(Dir.LEFT) in it } || groveMap.faces.none { edge.offset(Dir.RIGHT) in it } }

            val horizontalEdges = groveMap.faces.flatMap { listOf(Rect(it.x1, it.y1, it.x2, it.y1), Rect(it.x1, it.y2, it.x2, it.y2)) }
            val unconnectedHorizontalEdges = horizontalEdges
                .filter { edge -> groveMap.faces.none { edge.offset(Dir.UP) in it } || groveMap.faces.none { edge.offset(Dir.DOWN) in it } }

            val verticalEdgePairs = unconnectedVerticalEdges.sortedBy { it.y1 }.chunked(2).map { Pair(it[0], it[1]) }
            val horizontalEdgePairs = unconnectedHorizontalEdges.sortedBy { it.x1 }.chunked(2).map { Pair(it[0], it[1]) }

            edgePairs = mapOf(
                true to verticalEdgePairs,
                false to horizontalEdgePairs
            )
        }

        override fun wrap(p: P, dir: Dir): Pair<P, Dir> {
            edgePairs[dir.isHorizontal()]!!.first { p in it.first || p in it.second }.also { edgePair ->
                val fromEdge = if (p in edgePair.first) edgePair.first else edgePair.second
                val toEdge = if (fromEdge == edgePair.first) edgePair.second else edgePair.first
                return Pair(p.offset(toEdge.x1 - fromEdge.x1, toEdge.y1 - fromEdge.y1), dir)
            }
        }
    }

    class CubeWrapper(groveMap: GroveMap) : Wrapper() {

        override fun wrap(p: P, dir: Dir): Pair<P, Dir> {
            TODO("Not yet implemented")
        }

    }

    fun startTheTrial(groveMap: GroveMap, wrapper: Wrapper, moves: List<Move>, startP: P): Pair<P, Dir> {

        var dir = Dir.RIGHT
        var p = startP

        "Start at $p".logd()

        moves.forEach { move ->
            move.logv()
            for (step in 1..move.step) {

                var stepP = p.offset(dir)
                if (groveMap.faces.none { stepP in it }) {
                    val wrapResult = wrapper.wrap(p, dir)
                    stepP = wrapResult.first
                    dir = wrapResult.second
                    "Wrap to $stepP with direction $dir".logv()
                }

                val wall = groveMap.walls.firstOrNull { it == stepP }
                if (wall != null) {
                    "Bump into wall at $wall".logv()
                    break
                }

                p = stepP
            }

            "Stop at $p".logv()

            if (move.turn != null) {
                dir = dir.turn(move.turn)
                "Turn ${move.turn}, current direction = $dir".logv()
            }
        }

        "Final location: $p".logd()

        return Pair(p, dir)
    }

    val (grove, moves, startP) = loadGroveMap()
    val wrapper = TransportWrapper(grove)

    fun part1(): Int {
        myLogLevel = 1


        val (finalP, finalDir) = startTheTrial(
            groveMap = grove,
            wrapper = wrapper,
            moves = moves,
            startP = startP
        )

        val dirPassword = when (finalDir) {
            Dir.RIGHT -> 0
            Dir.DOWN -> 1
            Dir.LEFT -> 2
            Dir.UP -> 3
        }

        return 1000 * finalP.y + 4 * finalP.x + dirPassword
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}