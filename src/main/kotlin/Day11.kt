import java.io.File

fun main() {

    data class Monkey(val id: Int, val operation: (Long) -> Long, val testDivider: Long, val targetMonkeys: Pair<Int, Int>, val items: MutableList<Long>) {
        fun testId(item: Long) = if (item % testDivider == 0L) {
            targetMonkeys.first
        } else {
            targetMonkeys.second
        }
    }

    fun loadData() = File("src/main/resources/Day11.txt").readLines()

    fun String.findInts() = "[0-9]+".toRegex().findAll(this).flatMap { it.groupValues.map { s -> s.toInt() } }.toList()

    fun loadAsMonkeys(): List<Monkey> {
        val monkeys = mutableListOf<Monkey>()

        loadData().chunked(size = 7).forEach { monkeyLines ->

            val id = monkeyLines[0].findInts().first()
            val items = monkeyLines[1].findInts().map { it.toLong() }.toMutableList()

            val operationFormulaStr = monkeyLines[2].split("new = ")[1]
            val operation: (Long) -> Long = { worryLevel ->
                val s = operationFormulaStr.replace("old", worryLevel.toString())
                val (n1, n2) = s.findInts().map { it.toLong() }
                if ("+" in s) {
                    n1 + n2
                } else {
                    n1 * n2
                }
            }

            val testDivider = monkeyLines[3].findInts().first().toLong()
            val testTrueTargetId = monkeyLines[4].findInts().first()
            val testFalseTargetId = monkeyLines[5].findInts().first()

            monkeys.add(
                Monkey(
                    id = id,
                    operation = operation,
                    testDivider = testDivider,
                    targetMonkeys = Pair(testTrueTargetId, testFalseTargetId),
                    items = items
                )
            )
        }

        return monkeys
    }

    fun part1(): Long {
        val monkeys = loadAsMonkeys()
        val monkeyInspectCount = IntArray(monkeys.size).toMutableList()

        repeat(times = 20) {
            monkeys.forEach { monkey ->
                monkeyInspectCount[monkey.id] += monkey.items.size
                monkey.items.forEach { item ->
                    val itemNew = monkey.operation(item) / 3
                    val idNew = monkey.testId(itemNew)
                    monkeys.find { it.id == idNew }!!.items.add(itemNew)
                }
                monkey.items.clear()
            }
        }

        return monkeyInspectCount.sorted().takeLast(2).map { it.toLong() }.reduce { acc, i -> acc * i }.toLong()
    }

    fun part2(): Long {
        val monkeys = loadAsMonkeys()
        val monkeyInspectCount = IntArray(monkeys.size).toMutableList()
        val mod = monkeys.map { m -> m.testDivider }.reduce { acc, i -> acc * i }

        repeat(times = 10000) {
            monkeys.forEach { monkey ->
                monkeyInspectCount[monkey.id] += monkey.items.size
                monkey.items.forEach { item ->
                    val itemNew = monkey.operation(item) % mod
                    val idNew = monkey.testId(itemNew)
                    monkeys.find { m -> m.id == idNew }!!.items.add(itemNew)
                }
                monkey.items.clear()
            }
        }
        monkeyInspectCount.log()

        return monkeyInspectCount.sorted().takeLast(2).map { it.toLong() }.reduce { acc, i -> acc * i }
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}