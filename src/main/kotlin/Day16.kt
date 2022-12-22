import java.io.File
import kotlin.math.min

fun main() {

    class Valve(val name: String, val flowRate: Int) {
        var isOpened = false
        val connectedValves: MutableMap<Valve, Int> = mutableMapOf()

        override fun toString(): String {
            return "Valve($name, fr=$flowRate, to=${connectedValves.map { "${it.key.name}(${it.value})" }})"
        }
    }

    fun loadData() = File("src/main/resources/Day16_test.txt").readLines()

    fun loadAsValves(): List<Valve> {
        val valveDescriptions = loadData().map { line -> "[A-Z]{2}|[0-9]+".toRegex().findAll(line).map { it.groupValues[0] }.toList() }
        val valves = valveDescriptions.map { Valve(it[0], it[1].toInt()) }
        valves.zip(valveDescriptions).forEach { (v, vd) ->
            v.connectedValves.putAll(valves.filter { it.name in vd.subList(2, vd.size) }.associateWith { 1 })
        }
        return valves
    }

    fun removeValveFromConnection(valveToRemove: Valve, valves: MutableList<Valve>) {
        valves.remove(valveToRemove)
        valveToRemove.connectedValves.forEach { (neighbor, minutesToNeighbor) ->
            neighbor.connectedValves.remove(valveToRemove)
            valveToRemove.connectedValves.filter { it.key !== neighbor }.forEach { (anotherNeighbor, minutesToAnotherNeighbor) ->
                if (anotherNeighbor in neighbor.connectedValves) {
                    neighbor.connectedValves[anotherNeighbor] =
                        min(neighbor.connectedValves[anotherNeighbor]!!, minutesToNeighbor + minutesToAnotherNeighbor)
                } else {
                    neighbor.connectedValves[anotherNeighbor] = minutesToNeighbor + minutesToAnotherNeighbor
                }
            }
        }
    }

    fun greedySearch() {
        val tree = Tree()
        val TESTED = "tested"

        val valves: List<Valve> = loadAsValves()
        val startValve = valves.find { it.name == "AA" }!!
        var remainingValves: MutableList<Valve> = valves.toMutableList()
        valves.filter { it.flowRate == 0 && it !== startValve }.forEach { removeValveFromConnection(it, remainingValves) }
        val finalRemainingValves = remainingValves.toList()
        val finalValvesConnection = finalRemainingValves.associateWith { it.connectedValves.toList() }

        val shortestMinutesMap = mutableMapOf<String, MutableMap<String, Int>>()
        finalRemainingValves.forEach { v ->
            shortestMinutesMap.putIfAbsent(v.name, mutableMapOf(v.name to 0))
            val vMap = shortestMinutesMap[v.name]!!
            vMap.putAll(v.connectedValves.map { it.key.name to it.value })
            while (true) {
                val modification = mutableMapOf<String, Int>()
                vMap.forEach { (anotherV, minutes) ->
                    finalRemainingValves.first { it.name == anotherV }.connectedValves.forEach { (theOtherV, theOtherMinutes) ->
                        val minutesToTheOtherV = vMap[theOtherV.name]
                        if (minutesToTheOtherV == null || minutesToTheOtherV > minutes + theOtherMinutes) {
                            modification[theOtherV.name] = minutes + theOtherMinutes
                        }
                    }
                }
                if (modification.isEmpty()) {
                    break
                } else {
                    vMap.putAll(modification)
                }
            }
        }
        shortestMinutesMap.forEach { it.log() }

        fun Any.log() {

        }

        val bestMoves = mutableListOf<String>()
        var bestReleasedPressure = 0

        var round = 1
        while (true) {
            round++

            tree.node = tree.root

            remainingValves = finalRemainingValves.toMutableList()
            var currentValve = startValve
            remainingValves.forEach {
                it.connectedValves.apply { clear() }.putAll(finalValvesConnection[it]!!)
                it.isOpened = false
            }

            var releasedPressure = 0
            var minutesLeft = 30

            val moves = mutableListOf<String>()

            var noOpenMinutes = 0
            var noOpenMove: String? = currentValve.name

            while (minutesLeft > 0) {
                "$minutesLeft minutes left, current valve [${currentValve.name}]".log()

                val untestedMoves = tree.branches()?.filter { it.note != TESTED }?.map { it.name }
                val moveCandidates = untestedMoves ?: mutableListOf<String>().apply {
                    if (!currentValve.isOpened && currentValve.flowRate > 0) {
                        add("Open")
                    }
                    addAll(currentValve.connectedValves.filter { (neighbor, minutes) ->
                        val shortestMinutes = shortestMinutesMap[noOpenMove]?.get(neighbor.name)
                        if (shortestMinutes == null) {
                            true
                        } else {
                            noOpenMinutes + minutes <= shortestMinutes
                        }
                    }.map { it.key.name })
                    if (isEmpty()) {
                        add("Done")
                    }
                    tree.addBranches(*(this.toTypedArray()))
                }
                val move = moveCandidates.first()
                moves.add(move)
                tree.checkout(move)
                "Take action $move".log()

                val pressureReleasedPerMinute = valves.filter { it.isOpened }.sumOf { it.flowRate }

                when (move) {
                    "Open" -> {
                        releasedPressure += pressureReleasedPerMinute * 1
                        minutesLeft -= 1
                        currentValve.isOpened = true

                        noOpenMinutes = 0
                        noOpenMove = null
                    }

                    "Done" -> {
                        releasedPressure += pressureReleasedPerMinute * minutesLeft
                        minutesLeft = 0
                    }

                    else -> {
                        val nextValve = remainingValves.find { it.name == move }!!
                        val minutesCost = min(currentValve.connectedValves[nextValve]!!, minutesLeft)
                        noOpenMinutes += minutesCost
                        if (noOpenMove == null) {
                            noOpenMove = move
                        }

                        releasedPressure += pressureReleasedPerMinute * minutesCost
                        minutesLeft -= minutesCost

                        if (currentValve.isOpened || currentValve.flowRate == 0) {
                            removeValveFromConnection(currentValve, remainingValves)
                        }
                        currentValve = nextValve
                    }
                }

                val openedValvesStr = valves.filter { it.isOpened }.map { it.name }.toString()
                "Valves $openedValvesStr are open, released pressure = $releasedPressure".log()
            }

//            if (round % 1000 == 0) {
                "Round #$round: $releasedPressure pressure released with moves ${moves.joinToString("-")}".print()
//            }
            if (bestReleasedPressure < releasedPressure) {
                bestReleasedPressure = releasedPressure
                bestMoves.clear()
                bestMoves.addAll(moves)
            }
//            if (round % 10000 == 0) {
//                "Temporary best: $bestReleasedPressure pressure released with moves ${bestMoves.joinToString("-")}".print()
//            }

            var endFlag = false

            tree.node.note = TESTED
            while (tree.toParent()) {
                if (tree.branches()!!.all { it.note == TESTED }) {
                    tree.node.note = TESTED
                    if (tree.node == tree.root) {
                        endFlag = true
                    }
                } else {
                    break
                }
            }

            if (endFlag) {
                break
            }
        }

        "Best: $bestReleasedPressure pressure released with moves ${bestMoves.joinToString("-")}".print()
    }

    greedySearch()

//    val part1 = part1()
//    val part2 = part2()
//    println("part1 = $part1")
//    println("part2 = $part2")
}