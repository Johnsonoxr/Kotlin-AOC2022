import java.io.File

fun main() {
    fun loadData() = File("src/main/resources/Day01.txt").readLines()

    fun packedCalories(data: List<String>): MutableList<MutableList<Int>> {
        val calories = mutableListOf<MutableList<Int>>()
        data.forEach {
            val cal = it.toIntOrNull()
            if (cal != null) {
                if (calories.isEmpty()) {
                    calories.add(mutableListOf())
                }
                calories.last().add(cal)
            } else {
                calories.add(mutableListOf())
            }
        }
        return calories
    }

    fun part1(): Int {
        val caloriesPerElf = packedCalories(loadData()).flatMap { listOf(it.sum()) }
        return caloriesPerElf.max()
    }

    fun part2(): Int {
        val caloriesPerElf = packedCalories(loadData()).flatMap { listOf(it.sum()) }
        return caloriesPerElf.sorted().takeLast(3).sum()
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}