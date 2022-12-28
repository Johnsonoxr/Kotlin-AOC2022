import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

fun main() {

    data class P(val x: Int, val y: Int) {
        override fun toString(): String {
            return "P($x, $y)"
        }

        fun offset(dir: Dir): P = offset(dir.dx, dir.dy)

        fun offset(dx: Int, dy: Int): P = P(this.x + dx, this.y + dy)

        fun minkowskiDistance(p: P): Int = abs(x - p.x) + abs(y - p.y)
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

    class GroveMap(val faces: List<Rect>, val walls: Set<P>)

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

        return Triple(GroveMap(faces, walls), moves, startP!!)
    }

    class TeleportWrapper(groveMap: GroveMap) : Wrapper() {

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

    class CubicWrapper(groveMap: GroveMap) : Wrapper() {

        inner class Portal(val dir: Dir, val edge: Rect) {
            override fun toString() = "Portal(${dir}: $edge)"
        }

        val portals: List<Pair<Portal, Portal>>

        init {
            val efMap = mutableMapOf<Rect, Rect>()

            val ecMap = mutableMapOf<Rect, Set<P>>()
            val ceMap = mutableMapOf<P, MutableSet<Rect>>()

            val cornerGroups = mutableSetOf<MutableSet<P>>()

            val edgePairs = mutableSetOf<Set<Rect>>()

            groveMap.faces.forEach { face ->

                val corners = listOf(
                    P(face.x1, face.y1),
                    P(face.x1, face.y2),
                    P(face.x2, face.y2),
                    P(face.x2, face.y1)
                )
                val edges = mutableListOf<Rect>()
                corners.toMutableList().apply { add(corners[0]) }.windowed(2).forEach { (p1, p2) ->
                    val left = min(p1.x, p2.x)
                    val right = max(p1.x, p2.x)
                    val top = min(p1.y, p2.y)
                    val bottom = max(p1.y, p2.y)
                    val edge = Rect(left, top, right, bottom)
                    ecMap[edge] = setOf(p1, p2)
                    ceMap.putIfAbsent(p1, mutableSetOf())
                    ceMap.putIfAbsent(p2, mutableSetOf())
                    ceMap[p1]?.add(edge)
                    ceMap[p2]?.add(edge)
                    edges.add(edge)
                }

                efMap.putAll(edges.associateWith { face })
            }

            ceMap.keys.forEach { corner ->
                val group = cornerGroups.firstOrNull { corners -> corners.any { it.minkowskiDistance(corner) <= 2 } }
                if (group != null) {
                    group.add(corner)
                } else {
                    cornerGroups.add(mutableSetOf(corner))
                }
            }

            ecMap.keys.forEach { edge ->
                if (edgePairs.any { edge in it }) {
                    return@forEach
                }
                ecMap.keys.firstOrNull { e -> Dir.values().any { dir -> e.offset(dir) == edge } }?.also { edge2 ->
                    edgePairs.add(setOf(edge, edge2))
                }
            }

            var updated = true
            while (updated) {
                updated = false
                cornerGroups.filter { it.size == 3 }.forEach { cornerGroup ->
                    if (updated) {
                        return@forEach
                    }
                    val unpairedEdges = cornerGroup.flatMap { ceMap[it]!! }.filter { edge -> edgePairs.none { pair -> edge in pair } }
                    if (unpairedEdges.size == 2) {
                        edgePairs.add(unpairedEdges.toSet())
                        val unmergedCorners = unpairedEdges.flatMap { ecMap[it]!! }.filter { it !in cornerGroup }
                        val cornerGroupsToBeMerged = unmergedCorners.map { corner -> cornerGroups.first { corner in it } }.toSet()
                        if (cornerGroupsToBeMerged.size == 2) {
                            cornerGroups.removeAll(cornerGroupsToBeMerged)
                            cornerGroups.add(cornerGroupsToBeMerged.reduce { acc, ps -> acc.union(ps).toMutableSet() })
                        }
                        updated = true
                    }
                }
            }

            val portals = mutableListOf<Pair<Portal, Portal>>()

            fun dirFaceCenterToEdge(edge: Rect, face: Rect): Dir = when {
                edge.x1 > face.x1 -> Dir.RIGHT
                edge.x2 < face.x2 -> Dir.LEFT
                edge.y1 > face.y1 -> Dir.DOWN
                else -> Dir.UP
            }

            edgePairs.map { it.toList() }.forEach { (e1, e2) ->
                val face1 = efMap[e1]!!
                val face2 = efMap[e2]!!
                val dir1 = dirFaceCenterToEdge(e1, face1)
                val dir2 = dirFaceCenterToEdge(e2, face2)

                portals.add(Pair(Portal(dir1, e1), Portal(dir2.reverseDir(), e2)))
                portals.add(Pair(Portal(dir2, e2), Portal(dir1.reverseDir(), e1)))
            }

            this.portals = portals

            this.portals.forEach { "${it.first} -> ${it.second}".logd() }
        }

        override fun wrap(p: P, dir: Dir): Pair<P, Dir> {
            val (ptIn, ptOut) = portals.first { pair -> p in pair.first.edge && dir == pair.first.dir }

            val edgeOffset: Int = when (dir) {
                Dir.LEFT -> ptIn.edge.y2 - p.y
                Dir.UP -> p.x - ptIn.edge.x1
                Dir.RIGHT -> p.y - ptIn.edge.y1
                Dir.DOWN -> ptIn.edge.x2 - p.x
            }

            val pOut: P = when (ptOut.dir) {
                Dir.LEFT -> P(ptOut.edge.x1, ptOut.edge.y2 - edgeOffset)
                Dir.UP -> P(ptOut.edge.x1 + edgeOffset, ptOut.edge.y1)
                Dir.RIGHT -> P(ptOut.edge.x1, ptOut.edge.y1 + edgeOffset)
                Dir.DOWN -> P(ptOut.edge.x2 - edgeOffset, ptOut.edge.y1)
            }

            return Pair(pOut, ptOut.dir)
        }
    }

    fun startTheTrial(groveMap: GroveMap, wrapper: Wrapper, moves: List<Move>, startP: P): List<Pair<P, Dir>> {

        val movements = mutableListOf<Pair<P, Dir>>()

        var dir = Dir.RIGHT
        var p = startP

        movements.add(Pair(p, dir))

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

                movements.add(Pair(p, dir))
            }

            "Stop at $p".logv()

            if (move.turn != null) {
                dir = dir.turn(move.turn)
                "Turn ${move.turn}, current direction = $dir".logv()
            }
        }

        "Final location: $p".logd()

        return movements
    }

    val (grove, moves, startP) = loadGroveMap()

    fun plotTraces(traces: List<Pair<P, Dir>>) {

        val tracesGroupingByY = traces.groupBy { it.first.y }

        loadData().forEachIndexed { y0, line ->
            val y = y0 + 1
            var linePlot = line
            tracesGroupingByY[y]?.forEach { traceInY ->
                val x = traceInY.first.x
                linePlot = linePlot.replaceRange(x - 1, x, traceInY.second.plot())
            }
            linePlot.print()
        }
    }

    fun part1(): Int {
        myLogLevel = 1

        val wrapper = TeleportWrapper(grove)

        val traces = startTheTrial(
            groveMap = grove,
            wrapper = wrapper,
            moves = moves,
            startP = startP
        )

        val facingPassword = when (traces.last().second) {
            Dir.RIGHT -> 0
            Dir.DOWN -> 1
            Dir.LEFT -> 2
            Dir.UP -> 3
        }

        return 1000 * traces.last().first.y + 4 * traces.last().first.x + facingPassword
    }

    fun part2(): Int {
        myLogLevel = 1

        val wrapper = CubicWrapper(grove)

        val traces = startTheTrial(
            groveMap = grove,
            wrapper = wrapper,
            moves = moves,
            startP = startP
        )

        val facingPassword = when (traces.last().second) {
            Dir.RIGHT -> 0
            Dir.DOWN -> 1
            Dir.LEFT -> 2
            Dir.UP -> 3
        }

//        plotTraces(traces)

        return 1000 * traces.last().first.y + 4 * traces.last().first.x + facingPassword
    }

    measureTimeMillis {
        val part1 = part1()
        val part2 = part2()
        println("part1 = $part1")
        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}