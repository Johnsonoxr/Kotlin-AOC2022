import java.io.File

fun main() {
    fun loadData() = File("src/main/resources/Day04.txt").readLines()

    fun rangeStrToRange(rangeDescription: String): IntRange {
        val startAndEnd = rangeDescription.split("-").map { it.toInt() }
        return startAndEnd[0]..startAndEnd[1]
    }

    fun loadAsIntRangeOf2ElvesList(): List<Pair<IntRange, IntRange>> =
        loadData().map { it.split(",").let { des -> Pair(rangeStrToRange(des[0]), rangeStrToRange(des[1])) } }

    fun part1(): Int {
        val fullyContains = loadAsIntRangeOf2ElvesList()
            .filter { (elf1, elf2) -> elf1.union(elf2).let { it.size == elf1.count() || it.size == elf2.count() }}
        return fullyContains.count()
    }

    fun part2(): Int {
        val fullyContains = loadAsIntRangeOf2ElvesList()
            .filter { (elf1, elf2) -> elf1.intersect(elf2).isNotEmpty() }
        return fullyContains.count()
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}