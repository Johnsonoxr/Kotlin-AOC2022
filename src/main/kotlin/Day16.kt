import java.io.File
import kotlin.math.min

fun main() {

    class Valve(val name: String, val flowRate: Int) {
        var isOpened = false

        override fun toString(): String {
            return "Valve($name, fr=$flowRate)"
        }
    }

    fun loadData() = File("src/main/resources/Day16.txt").readLines()

    fun loadValveTimeMap(): Map<Valve, Map<Valve, Int>> {

        val valveDescriptions = loadData().map { line -> "[A-Z]{2}|[0-9]+".toRegex().findAll(line).map { it.groupValues[0] }.toList() }
        val valves = valveDescriptions.map { Valve(it[0], it[1].toInt()) }

        val timeMap = mutableMapOf<Valve, MutableMap<Valve, Int>>()
        valves.zip(valveDescriptions).forEach { (v, vd) ->
            val neighborValves = vd.subList(2, vd.size).map { neighborName -> valves.first { it.name == neighborName } }
            timeMap[v] = neighborValves.associateWith { 1 }.toMutableMap()
        }
        timeMap.keys.forEach { v -> timeMap[v]?.put(v, 0) }

        valves.filter { it.flowRate != 0 || it.name == "AA" }.forEach { valve ->
            val vMap = timeMap[valve] ?: return@forEach
            while (true) {
                val modify = mutableMapOf<Valve, Int>()
                vMap.forEach { (v1, t1) ->
                    timeMap[v1]!!.forEach { (v2, t2) ->
                        val t = vMap[v2]
                        if (t == null || t > t1 + t2) {
                            modify[v2] = t1 + t2
                        }
                    }
                }
                if (modify.isEmpty()) {
                    break
                } else {
                    vMap.putAll(modify)
                }
            }
            vMap.entries.removeIf { it.key.flowRate == 0 }
        }

        return timeMap.filterKeys { it.flowRate != 0 || it.name == "AA" }
    }

    loadValveTimeMap().forEach { (k, v) -> "${k.name} -> ${v.map { "${it.key.name}(${it.value})" }.joinToString(", ")}".print() }

    fun factorial(n: Int): Long {
        return if (n == 0) {
            1L
        } else {
            (1L..n.toLong()).reduce { acc, i -> acc * i }
        }
    }

    fun <T> List<T>.changeOrder(seed: Long): List<T> {
        if (size == 1) {
            return this
        }
        val mList = toMutableList()
        val divider = factorial(size - 1)
        val first = mList.removeAt((seed / divider).toInt())
        return mutableListOf(first).also { it.addAll(mList.changeOrder(seed % divider)) }
    }

    fun describeValveChain(valves: List<Valve>): String = valves.joinToString("-") { it.name }

    fun greedySearch() {
        val directTimeMap = loadValveTimeMap()
        val startValve = directTimeMap.keys.find { it.name == "AA" }!!

        fun Any.log() {

        }

        val bestMoves = mutableListOf<Valve>()
        var bestReleasedPressure = 0

        val sortedValves = directTimeMap.keys.filter { it.flowRate != 0 }.sortedBy { it.name }

        var seedCount = 0L
        val maxSeed: Long = factorial(sortedValves.size)
        var sortSeed = 0L
        while (sortSeed < maxSeed) {
            val valves = sortedValves.changeOrder(sortSeed)
            var step = 0

            val remainingValves = valves.toMutableList()
            remainingValves.forEach { it.isOpened = false }
            var currentValve = startValve

            var releasedPressure = 0
            var minutesLeft = 30

            while (minutesLeft > 0) {
                "$minutesLeft minutes left, current valve [${currentValve.name}]".log()

                val pressureReleasedPerMinute = valves.filter { it.isOpened }.sumOf { it.flowRate }

                when {
                    !currentValve.isOpened -> {
                        releasedPressure += pressureReleasedPerMinute * 1
                        minutesLeft -= 1
                        currentValve.isOpened = true
                    }
                    step < valves.size -> {
                        val nextValve = valves[step++]
                        val minutesCost = min(directTimeMap[currentValve]?.get(nextValve) ?: Int.MAX_VALUE, minutesLeft)

                        releasedPressure += pressureReleasedPerMinute * minutesCost
                        minutesLeft -= minutesCost

                        currentValve = nextValve
                    }
                    else -> {
                        releasedPressure += pressureReleasedPerMinute * minutesLeft
                        minutesLeft = 0
                    }
                }

                val openedValvesStr = valves.filter { it.isOpened }.map { it.name }.toString()
                "Valves $openedValvesStr are open, released pressure = $releasedPressure".log()
            }
            val executedValves = valves.take(step)

            "Seed #$sortSeed: $releasedPressure pressure released with moves ${describeValveChain(executedValves)}".print()
            if (bestReleasedPressure < releasedPressure) {
                bestReleasedPressure = releasedPressure
                bestMoves.clear()
                bestMoves.addAll(executedValves)
            }

            val redundantStepCount = valves.size - executedValves.size
            sortSeed += if (redundantStepCount > 0) {
                "Jump seeds with $redundantStepCount redundant steps.".log()
                factorial(redundantStepCount)
            } else {
                1
            }
            seedCount++
        }

        "\nTotal tested seeds: $seedCount".print()
        "Best: $bestReleasedPressure pressure released with moves ${describeValveChain(bestMoves)}".print()
    }

    greedySearch()

//    val part1 = part1()
//    val part2 = part2()
//    println("part1 = $part1")
//    println("part2 = $part2")
}