import java.io.File

fun main() {
    fun loadData() = File("src/main/resources/Day03.txt").readLines()

    fun loadCompartmentPairList() = loadData().map { Pair(it.substring(0, it.length / 2), it.substring(it.length / 2)) }

    fun itemToPriority(item: Char) = when {
        item.isLowerCase() -> item.code - 'a'.code + 1
        item.isUpperCase() -> item.code - 'A'.code + 27
        else -> 0
    }

    fun part1(): Int {
        val sharedItems = loadCompartmentPairList().map { (c1, c2) -> c1.toSet().intersect(c2.toSet()).firstOrNull() }
        return sharedItems.filterNotNull().sumOf { itemToPriority(it) }
    }

    fun part2(): Int {
        val groupOf3ElvesList = loadData().windowed(size = 3, step = 3)
        val badges = groupOf3ElvesList.map { it[0].toSet().intersect(it[1].toSet()).intersect(it[2].toSet()).firstOrNull() }
        return badges.filterNotNull().sumOf { itemToPriority(it) }
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}