import java.io.File

fun main() {

    class Valve(val name: String, val flowRate: Int) {

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

    fun describeValveChain(valves: List<Valve>): String = valves.joinToString("-") { it.name }

    val valveTimeMap = loadValveTimeMap()
    valveTimeMap.forEach { (k, v) -> "${k.name} -> ${v.map { "${it.key.name}(${it.value})" }.joinToString(", ")}".print() }

    fun greedySearch(previousValve: Valve, valvesToSolve: Collection<Valve>, minutes: Int): Pair<List<Valve>?, Int> {

        var bestPressureReleased = 0
        var bestValveList: MutableList<Valve>? = null

        valvesToSolve.forEach { firstValve ->
            val minutesLeft = minutes - valveTimeMap[previousValve]!![firstValve]!! - 1
            if (minutesLeft <= 0) {
                return@forEach
            }
            val pressure = minutesLeft * firstValve.flowRate

            val valvesLeft = valvesToSolve.toMutableList().apply { remove(firstValve) }

            val (valvesSolved, pressureSolved) = greedySearch(firstValve, valvesLeft, minutesLeft)

            if (bestPressureReleased < pressure + pressureSolved) {
                bestPressureReleased = pressure + pressureSolved
                bestValveList = mutableListOf(firstValve)
                valvesSolved?.also { bestValveList?.addAll(valvesSolved) }
            }
        }

        return Pair(bestValveList, bestPressureReleased)
    }

    fun part1(): Int {
        val (bestValveSeq, bestPressureReleased) = greedySearch(
            valveTimeMap.keys.first { it.name == "AA" },
            valveTimeMap.keys.filter { it.name != "AA" },
            30
        )
        "Best: $bestPressureReleased with sequence ${describeValveChain(bestValveSeq!!)}".print()

        return bestPressureReleased
    }

    fun part2(): Int {
        class CombinationGenerator<T>(private val list: List<T>, private val k: Int) {
            private val combination = MutableList(k) { it }

            fun next(): Boolean {
                var idx = k - 1
                while (idx >= 0) {
                    when {
                        combination[idx] < list.size - (k - idx - 1) - 1 -> {
                            combination[idx] += 1
                            (idx + 1 until k).forEach { i ->
                                combination[i] = combination[i - 1] + 1
                            }
                            return true
                        }

                        else -> idx -= 1
                    }
                }
                return false
            }

            fun getList() = combination.map { list[it] }
        }

        val startValve = valveTimeMap.keys.find { it.name == "AA" }!!
        val valves = valveTimeMap.keys.filter { it.name != "AA" }.toList()

        var bestPressureReleased = 0
        var myBestSteps = emptyList<Valve>()
        var elephantsBestSteps = emptyList<Valve>()

        var totalCombinations = 0
        val dividedMax = valves.size / 2
        (1..dividedMax).forEach { myValveSize ->
            val combinationGenerator = CombinationGenerator(valves, myValveSize)
            do {
                val myValves = combinationGenerator.getList()
                val elephantValves = valves.filter { it !in myValves }

                val (mySteps, myPressureReleased) = greedySearch(startValve, myValves, 26)
                val (elephantsSteps, elephantsPressureReleased) = greedySearch(startValve, elephantValves, 26)

                val pressureReleased = myPressureReleased + elephantsPressureReleased

                ("Given the combination of mine=${myValves.map { it.name }} and elephants=${elephantValves.map { it.name }}," +
                        " my steps ${describeValveChain(mySteps!!)} and elephant's steps ${describeValveChain(elephantsSteps!!)}" +
                        " gives $pressureReleased pressure released!!").print()

                if (bestPressureReleased < pressureReleased) {
                    bestPressureReleased = pressureReleased
                    myBestSteps = mySteps.toList()
                    elephantsBestSteps = elephantsSteps.toList()
                }
                totalCombinations += 1
            } while (combinationGenerator.next())
        }

        "Total combinations = $totalCombinations".print()
        "Best of all: $bestPressureReleased pressure released.".print()
        "My steps: ${describeValveChain(myBestSteps)}".print()
        "Elephant's steps: ${describeValveChain(elephantsBestSteps)}".print()

        return bestPressureReleased
    }

    val part1 = part1()
    val part2 = part2()
    println("part1 = $part1")
    println("part2 = $part2")
}