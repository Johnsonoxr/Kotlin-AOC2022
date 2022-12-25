import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.system.measureTimeMillis

enum class Material(val str: String) {
    Ore(str = "ore"),
    Clay(str = "clay"),
    Obsidian(str = "obsidian"),
    Geode(str = "geode");

    companion object {
        fun fromStr(str: String) = values().first { it.str == str }
    }
}

fun main() {

    class Robot(val harvesting: Material, val cost: Map<Material, Int>) {
        override fun toString(): String {
            return "$name(costs=${cost.map { (m, n) -> "$n ${m.name}s" }})"
        }

        val name = "${harvesting}Robot"
    }

    fun loadData() = File("src/main/resources/Day19t.txt").readLines()

    fun loadBlueprints(): List<Map<Material, Robot>> {
        val blueprints = mutableListOf<Map<Material, Robot>>()
        val blueprintSplitRegex = "Blueprint [0-9]+:".toRegex()
        val materialRetrieveRegex = "(${Material.values().joinToString("|") { it.str }})|[0-9]+".toRegex()

        loadData().joinToString("").split(blueprintSplitRegex).filter { it.isNotEmpty() }.forEach { line ->
            val blueprint = mutableMapOf<Material, Robot>()
            blueprints.add(blueprint)

            line.split(".").filter { it.isNotEmpty() }.map { linePerRobot ->
                val words = materialRetrieveRegex.findAll(linePerRobot).map { it.groupValues[0] }.toList()
                val produce = Material.fromStr(words.first())
                val costs = words.subList(1, words.size).chunked(2).associate { (n, material) -> Material.fromStr(material) to n.toInt() }
                blueprint[produce] = Robot(produce, costs)
            }
        }
        return blueprints
    }

    loadBlueprints().forEach { it.logi() }

    fun harvest(
        bpRobots: List<Robot>,
        bestFutureResInAllMinutes: MutableMap<Int, MutableSet<MutableMap<Material, Int>>>,
        harvestingRobots: List<Robot>,
        resources: Map<Material, Int>,
        minutesLeft: Int
    ): Pair<List<Robot>, Map<Material, Int>> {

        if (minutesLeft < 1) {
            return Pair(harvestingRobots, resources)
        }

        val futureRes = resources.toMutableMap()
        harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount().forEach { (m, n) ->
            futureRes[m] = futureRes.getOrDefault(m, defaultValue = 0) + n * minutesLeft
        }

        bestFutureResInAllMinutes.putIfAbsent(minutesLeft, mutableSetOf())
        val bestFutureRes = bestFutureResInAllMinutes[minutesLeft]!!
        if (bestFutureRes.any { res -> futureRes.all { (m, n) -> n <= res.getOrDefault(m, defaultValue = 0) } }) {
            return Pair(harvestingRobots, resources)
        } else {
            bestFutureRes.removeIf { res -> futureRes.all { (m, n) -> n >= res.getOrDefault(m, defaultValue = 0) } }
            bestFutureRes.add(futureRes)
            "Best future(${bestFutureRes.size}) updated: $minutesLeft with $futureRes".logi()
        }

        "Current robots: ${harvestingRobots.map { it.harvesting }}".logv()
        "Current resources: $resources".logv()

        var bestRobots = harvestingRobots
        var bestResources = resources
            .toMutableMap()
            .also { res ->
                harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount().forEach { (m, n) ->
                    res[m] = res.getOrDefault(m, defaultValue = 0) + n
                }
            }.toMap()

        bpRobots.forEach { robotToBuild ->

            val resourcesPerMinute = harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount()

            val minutesWaitForBuild = robotToBuild.cost.maxOf { (m, n) ->
                val resLack = max(0, n - (resources[m] ?: 0))
                if (resLack == 0) return@maxOf 1

                val resPerMinute = resourcesPerMinute[m] ?: 0
                if (resPerMinute == 0) return@maxOf Int.MAX_VALUE

                return@maxOf ceil(resLack.toDouble() / resPerMinute).toInt() + 1
            }

            if (minutesWaitForBuild >= minutesLeft) {
                return@forEach
            }

            val resourcesLeft = resources.toMutableMap()

            resourcesPerMinute.forEach { (m, n) ->
                resourcesLeft[m] = (resourcesLeft[m] ?: 0) + n * minutesWaitForBuild - robotToBuild.cost.getOrDefault(m, 0)
            }

            val (newRobots, newResource) = harvest(
                bpRobots = bpRobots,
                bestFutureResInAllMinutes = bestFutureResInAllMinutes,
                harvestingRobots = harvestingRobots.toMutableList().apply { add(robotToBuild) },
                resources = resourcesLeft,
                minutesLeft = minutesLeft - minutesWaitForBuild
            )

            if (bestResources.getOrDefault(Material.Geode, defaultValue = 0) < newResource.getOrDefault(Material.Geode, defaultValue = 0)) {
                bestResources = newResource
                bestRobots = newRobots
//                ("Best progress updated: ${bestResources.getOrDefault(Material.Geode, defaultValue = 0)}" +
//                        " with ${bestRobots.joinToString("-") { it.harvesting.name }}").logd()
            }
        }

        return Pair(bestRobots, bestResources)
    }

    fun part1(): Int {
        myLogLevel = 2

        return loadBlueprints().mapIndexed { idx, bp ->
            val bpIdx = idx + 1
            val (robots, resources) = harvest(
                bpRobots = bp.values.toList(),
                bestFutureResInAllMinutes = mutableMapOf(),
                harvestingRobots = listOf(bp[Material.Ore]!!),
                resources = emptyMap(),
                minutesLeft = 24
            )
            val geodeOpened = resources[Material.Geode] ?: 0
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed bpIdx * geodeOpened
        }.reduce { acc, i -> acc + i }
    }

    measureTimeMillis {
        val part1 = part1()
//    val part2 = part2()
        println("part1 = $part1")
//    println("part2 = $part2")
    }.also { it.print() }
}