import java.io.File
import kotlin.math.abs
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

    fun loadData() = File("src/main/resources/Day23.txt").readLines()

    fun loadElves(): List<P> {

        val elves = mutableListOf<P>()
        val elfRegex = "#".toRegex()

        loadData().forEachIndexed { y, line ->
            elves.addAll(elfRegex.findAll(line).map { it.range.first }.map { P(it, y) })
        }

        return elves
    }

    fun part1(): Int {

        val elves = loadElves().toMutableList()
        val lookDirs = mutableListOf(Dir.UP, Dir.DOWN, Dir.LEFT, Dir.RIGHT)
        val lookOffsets = mapOf(
            Dir.UP to setOf(P(-1, -1), P(0, -1), P(1, -1)),
            Dir.DOWN to setOf(P(-1, 1), P(0, 1), P(1, 1)),
            Dir.LEFT to setOf(P(-1, -1), P(-1, 0), P(-1, 1)),
            Dir.RIGHT to setOf(P(1, -1), P(1, 0), P(1, 1))
        )

        fun looksGoodToMe(me: P, look: Dir): Boolean = lookOffsets[look]!!.all { offset -> me.offset(offset.x, offset.y) !in elves }

        repeat(times = 10) { round ->

            val nextElves: MutableList<Pair<P, P>> = mutableListOf()

            //  first half round
            elves.forEach { elf ->
                val dirsThatLooksGood = lookDirs.filter { dir -> looksGoodToMe(elf, dir) }

                if (dirsThatLooksGood.size == 4) {  // don't want to move
                    nextElves.add(Pair(elf, elf))
                } else {
                    val nextElf = dirsThatLooksGood.firstOrNull()?.let { dir -> elf.offset(dir) }
                    nextElves.add(Pair(elf, nextElf ?: elf))
                }
            }

            //  second half round
            elves.clear()
            nextElves.groupBy { it.second }.forEach { (_, elvesWhoWantToEnterP) ->
                if (elvesWhoWantToEnterP.size > 1) {
                    elves.addAll(elvesWhoWantToEnterP.map { it.first }) //  stay where you were
                } else {
                    elves.add(elvesWhoWantToEnterP.first().second)
                }
            }

            lookDirs.add(lookDirs.removeFirst())
        }

        val w = elves.maxOf { it.x } - elves.minOf { it.x } + 1
        val h = elves.maxOf { it.y } - elves.minOf { it.y } + 1

        return w * h - elves.size
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}