import java.io.File
import kotlin.math.min

fun main() {

    class Valve(val name: String, val flowRate: Int) {
        var isOpened = false
        val connectedValves: MutableMap<Valve, Int> = mutableMapOf()

        override fun toString(): String {
            return "Valve($name, fr=$flowRate, to=${connectedValves.map { "${it.key.name}-${it.value}" }})"
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
        val valves = loadAsValves()
        val mutableValves = valves.toMutableList()
        var currentValve = valves.find { it.name == "AA" }!!

        valves.filter { it.flowRate == 0 && it !== currentValve }.forEach { removeValveFromConnection(it, mutableValves) }
//        mutableValves.forEach { it.log() }

        val root: MutableMap<String, Any?> = mutableMapOf()
        var node: MutableMap<String, Any?> = root
        var previousMoves: List<String>? = null

        var releasedPressure = 0
        var minutesLeft = 30

        var bestMoves: List<String>? = null
        var bestReleasedPressure = 0

//        while (true) {
//            var previousMovesPtr = 0
            val moves = mutableListOf<String>()

            var prevMove: String? = null
            while (minutesLeft > 0) {
                "$minutesLeft minutes left, current valve [${currentValve.name}]".log()

                val possibleMove = mutableListOf<String>()
                if (!currentValve.isOpened && currentValve.flowRate > 0) {
                    possibleMove.add("Open")
                }
                possibleMove.addAll(currentValve.connectedValves.keys.filter { it.name != prevMove }.map { it.name })
                if (possibleMove.isEmpty()) {
                    possibleMove.add("Done")
                }
                val move = possibleMove.first()

                node.putAll(possibleMove.associateWith { mutableMapOf() })
                node = node[move] as MutableMap<String, Any?>

                prevMove = move

                val pressureReleasedPerMinute = valves.filter { it.isOpened }.sumOf { it.flowRate }

                when (move) {
                    "Open" -> {
                        releasedPressure += pressureReleasedPerMinute * 1
                        minutesLeft -= 1
                        currentValve.isOpened = true
                    }
                    "Done" -> {
                        releasedPressure += pressureReleasedPerMinute * minutesLeft
                        minutesLeft = 0
                    }
                    else -> {
                        val nextValve = mutableValves.find { it.name == move }!!
                        val minutesCost = min(currentValve.connectedValves[nextValve]!!, minutesLeft)

                        releasedPressure += pressureReleasedPerMinute * minutesCost
                        minutesLeft -= minutesCost

                        if (currentValve.isOpened || currentValve.flowRate == 0) {
                            removeValveFromConnection(currentValve, mutableValves)
                        }
                        currentValve = nextValve
                    }
                }

                val openedValvesStr = valves.filter { it.isOpened }.map { it.name }.toString()
                "Valves $openedValvesStr are open, released pressure = $releasedPressure".log()

                "Take action $move\n".log()
            }

            "$releasedPressure pressure released.".log()

            if (releasedPressure > bestReleasedPressure) {
                bestReleasedPressure = releasedPressure
                bestMoves = moves.toList()
            }

            previousMoves = moves.toList()
//        }
    }

    greedySearch()

//    val part1 = part1()
//    val part2 = part2()
//    println("part1 = $part1")
//    println("part2 = $part2")
}