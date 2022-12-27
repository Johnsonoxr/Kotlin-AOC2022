import java.io.File
import kotlin.system.measureTimeMillis

fun main() {

    data class P(val x: Int, val y: Int) {
        override fun toString(): String {
            return "P($x, $y)"
        }

        fun offset(dir: Dir): P = P(x + dir.dx, y + dir.dy)
    }

    data class Rect(var x1: Int, var y1: Int, var x2: Int, var y2: Int) {

        val w: Int
            get() = this.x2 - this.x1 + 1

        val h: Int
            get() = this.y2 - this.y1 + 1

        operator fun contains(p: P): Boolean {
            return p.x in x1..x2 && p.y in y1..y2
        }

        override fun toString(): String {
            return "Rect($x1, $y1 ~ $x2, $y2)"
        }
    }

    data class Move(val step: Int, val turn: Turn?)

    class GroveMap(val w: Int, val h: Int, val deepestInferno: List<Rect>, val walls: Set<P>)

    fun loadData() = File("src/main/resources/Day22.txt").readLines()

    fun loadGroveMap(): Triple<GroveMap, List<Move>, P> {

        val lines = loadData()
        val splitter = lines.indexOfFirst { it.isEmpty() }

        val groveWidth = lines.take(splitter).maxOf { it.length }
        val groveLines = lines.take(splitter).map { it + " ".repeat(groveWidth - it.length) }

        val infernos = mutableListOf<Rect>()
        val infernoRegex = " *".toRegex()

        val walls = mutableSetOf<P>()
        val wallRegex = "#".toRegex()

        var startP: P? = null

        groveLines.forEachIndexed { idx, line ->
            val y = idx + 1
            val infernoFractions = infernoRegex.findAll(line).toList()
                .filter { it.value.isNotEmpty() }
                .map { Rect(it.range.first + 1, y, it.range.last + 1, y) }

            infernoFractions.forEach { fraction ->
                val connectedInferno = infernos.firstOrNull { fraction.x1 == it.x1 && fraction.x2 == it.x2 && it.y2 + 1 == y }
                if (connectedInferno != null) {
                    connectedInferno.y2 = y
                } else {
                    infernos.add(fraction)
                }
            }

            walls.addAll(wallRegex.findAll(line).map { P(it.range.first + 1, y) })

            if (startP == null) {
                startP = P(line.indexOfFirst { it.toString() == "." } + 1, y)
            }
        }

        val moves = "[0-9]+|[L|R]".toRegex().findAll(lines.last())
            .map { it.value }.toList().chunked(2)
            .map {
                when (it.size) {
                    2 -> Move(it[0].toInt(), Turn.valueOf(it[1]))
                    else -> Move(it[0].toInt(), null)
                }
            }

        return Triple(GroveMap(groveWidth, groveLines.size, infernos, walls), moves, startP!!)
    }

    fun isOutsideTheMap(groveMap: GroveMap, p: P): Boolean = p.x <= 0 || p.x > groveMap.w || p.y <= 0 || p.y > groveMap.h

    fun wrap(groveMap: GroveMap, p: P, dir: Dir): P {
        var validP = P(p.x, p.y)
        while (true) {
            val nextP = validP.offset(dir.reverseDir())
            if (isOutsideTheMap(groveMap, nextP) || groveMap.deepestInferno.any { nextP in it }) {
                return validP
            }
            validP = nextP
        }
    }

    fun part1(): Int {
        myLogLevel = 0

        val (grove, moves, startP) = loadGroveMap()
        var dir = Dir.RIGHT
        var p = startP

        "Start at $p".logd()

        moves.forEach { move ->
            move.print()
            var newP = P(p.x, p.y)

            for (step in 1..move.step) {

                newP = newP.offset(dir)

                if (isOutsideTheMap(grove, newP) || grove.deepestInferno.any { newP in it }) {
                    newP = wrap(grove, newP, dir)
                    "Wrap to $newP".logv()
                }

                val wall = grove.walls.firstOrNull { it == newP }
                if (wall != null) {
                    "Bump into wall at $wall".logv()
                    break
                }

                p = newP
            }

            "Stop at $p".logv()

            if (move.turn != null) {
                dir = dir.turn(move.turn)
                "Turn ${move.turn}, current direction = $dir".logv()
            }
        }

        "Final location: $p".logd()

        val dirPassword = when (dir) {
            Dir.RIGHT -> 0
            Dir.DOWN -> 1
            Dir.LEFT -> 2
            Dir.UP -> 3
        }

        return 1000 * p.y + 4 * p.x + dirPassword
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}