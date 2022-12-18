import java.io.File
import java.util.Stack


data class Step(val move: Int, val from: Char, val to: Char)

fun main() {
    fun loadData() = File("src/main/resources/Day05.txt").readLines()

    fun loadAsPairOfStackMapAndSteps(): Pair<MutableMap<Char, MutableList<Char>>, List<Step>> {
        val lines = loadData()
        val stackMap = mutableMapOf<Char, MutableList<Char>>()

        val lineSplitIdx = lines.indexOfFirst { it.startsWith("move") }

        val stackLines = lines.subList(0, lineSplitIdx - 1).reversed()

        stackLines[0].forEachIndexed { idx, stackLabelChar ->
            if (!stackLabelChar.isDigit()) {
                return@forEachIndexed
            }
            stackMap[stackLabelChar] = Stack()
            stackLines.subList(1, stackLines.size).forEach { line ->
                line.getOrNull(idx)?.let { if (it.isWhitespace()) null else it }?.also { cargo ->
                    stackMap[stackLabelChar]?.add(cargo)
                }
            }
        }

        val stepLines = lines.subList(lineSplitIdx, lines.size)

        val steps = stepLines.map { line ->
            val numList = line.replace("move ", "")
                .replace(" from ", ",")
                .replace(" to ", ",")
                .split(",")
            return@map Step(numList[0].toInt(), numList[1].toCharArray()[0], numList[2].toCharArray()[0])
        }

        return Pair(stackMap, steps)
    }

    fun part1(): String {
        val (stacks, steps) = loadAsPairOfStackMapAndSteps()

        steps.forEach { step ->
            for (i in 0 until step.move) {
                stacks[step.to]?.add(stacks[step.from]!!.removeLast())
            }
        }

        return stacks.keys.sorted().map { k -> stacks[k]!!.last() }.joinToString("")
    }

    fun part2(): String {
        val (stacks, steps) = loadAsPairOfStackMapAndSteps()

        steps.forEach { step ->
            val stackFrom = stacks[step.from]!!
            val stackTo = stacks[step.to]!!
            val pop = stackFrom.subList(stackFrom.size - step.move, stackFrom.size)

            stackTo.addAll(pop)
            pop.clear()
        }

        return stacks.keys.sorted().map { k -> stacks[k]!!.last() }.joinToString("")
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}