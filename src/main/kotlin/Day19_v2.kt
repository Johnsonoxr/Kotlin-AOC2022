import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.system.measureTimeMillis

fun main() {

    class Robot(val harvesting: Material, val cost: Map<Material, Int>) {
        override fun toString(): String {
            return "$name(costs=${cost.map { (m, n) -> "$n ${m.name}s" }})"
        }

        val name = "${harvesting}Robot"
    }

    fun loadData() = File("src/main/resources/Day19.txt").readLines()

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

    fun buildRobot(targetRobot: Robot, harvestingRobots: List<Robot>, resources: Map<Material, Int>): Pair<Int?, Map<Material, Int>> {
        val resourcesPerMinute = harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount()

        val minutes = targetRobot.cost.maxOf { (m, n) ->
            val resLack = max(0, n - (resources[m] ?: 0))
            if (resLack == 0) return@maxOf 1

            val resPerMinute = resourcesPerMinute[m] ?: 0
            if (resPerMinute == 0) return Pair(null, resources)

            return@maxOf ceil(resLack.toDouble() / resPerMinute).toInt() + 1
        }
        val remainingResources = resources.toMutableMap().also { res ->
            resourcesPerMinute.forEach { (m, n) -> res[m] = res.getOrDefault(m, defaultValue = 0) + n * minutes }
            targetRobot.cost.forEach { (m, n) -> res[m] = res.getOrDefault(m, defaultValue = 0) - n }
        }
        return Pair(minutes, remainingResources)
    }

    fun maxGeodeGet(bpRobots: List<Robot>, harvestingRobots: List<Robot>, resources: Map<Material, Int>, minutesLeft: Int): Int {
        return when (minutesLeft) {
            0 -> resources[Material.Geode] ?: 0
            1 -> {
                val byRobotHarvesting = harvestingRobots.count { it.harvesting == Material.Geode }
                byRobotHarvesting + (resources[Material.Geode] ?: 0)
            }

            else -> {
                val robot = bpRobots.first { it.harvesting == Material.Geode }
                val byRobotHarvesting = harvestingRobots.count { it.harvesting == Material.Geode }
                val (minutesWait, res) = buildRobot(robot, harvestingRobots, resources)
                if (minutesWait != null && minutesLeft > minutesWait) {
                    if (minutesLeft - minutesWait > 1) {
                        maxGeodeGet(bpRobots, harvestingRobots.toMutableList().apply { add(robot) }, res, minutesLeft - minutesWait)
                    } else {
                        byRobotHarvesting * minutesLeft + 1 * (minutesLeft - minutesWait) + (res[Material.Geode] ?: 0)
                    }
                } else {
                    byRobotHarvesting * minutesLeft + (res[Material.Geode] ?: 0)
                }
            }
        }
    }

    fun harvest(
        bpRobots: List<Robot>,
        earlyBreak: IntArray,
        harvestingRobots: List<Robot>,
        resources: Map<Material, Int>,
        minutesLeft: Int
    ): Pair<List<Robot>, Map<Material, Int>> {

        if (minutesLeft < 1) {
            return Pair(harvestingRobots, resources)
        }

        if (minutesLeft < earlyBreak[1]) {
            if (maxGeodeGet(bpRobots, harvestingRobots, resources, minutesLeft) < (earlyBreak.firstOrNull() ?: 0)) {
                return Pair(harvestingRobots, resources)
            }
        }

        "Current robots: ${harvestingRobots.map { it.harvesting }}".logv()
        "Current resources: $resources".logv()

        var bestRobots = harvestingRobots
        var bestResources = resources
            .toMutableMap()
            .also { res ->
                harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount().forEach { (m, n) ->
                    res[m] = res.getOrDefault(m, defaultValue = 0) + n * minutesLeft
                }
            }.toMap()

        bpRobots.forEach { robotToBuild ->

            val (minutesWait, remainingResources) = buildRobot(robotToBuild, harvestingRobots, resources)

            if (minutesWait == null || minutesWait >= minutesLeft) {
                return@forEach
            }

            val (newRobots, newResource) = harvest(
                bpRobots = bpRobots,
                earlyBreak = earlyBreak,
                harvestingRobots = harvestingRobots.toMutableList().apply { add(robotToBuild) },
                resources = remainingResources,
                minutesLeft = minutesLeft - minutesWait
            )

            if (bestResources.getOrDefault(Material.Geode, defaultValue = 0) < newResource.getOrDefault(Material.Geode, defaultValue = 0)) {
                bestResources = newResource
                bestRobots = newRobots
            }
        }

        val geodes = bestResources[Material.Geode] ?: 0
        if (geodes > earlyBreak[0]) {
            earlyBreak[0] = geodes
            ("Best progress updated: ${bestResources.getOrDefault(Material.Geode, defaultValue = 0)}" +
                    " with ${bestRobots.joinToString("-") { it.harvesting.name }}").logi()
        }

        return Pair(bestRobots, bestResources)
    }

    fun part1(): Int {
        myLogLevel = 2

        return loadBlueprints().mapIndexed { idx, bp ->
            val bpIdx = idx + 1
            val (robots, resources) = harvest(
                bpRobots = bp.values.toList(),
                earlyBreak = intArrayOf(0, 5),
                harvestingRobots = listOf(bp[Material.Ore]!!),
                resources = emptyMap(),
                minutesLeft = 24
            )
            val geodeOpened = resources[Material.Geode] ?: 0
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed bpIdx * geodeOpened
        }.reduce { acc, i -> acc + i }
    }

    fun part2(): Int {
        myLogLevel = 2

        return loadBlueprints().take(3).mapIndexed { idx, bp ->
            val bpIdx = idx + 1
            val (robots, resources) = harvest(
                bpRobots = bp.values.toList(),
                earlyBreak = intArrayOf(0, 15),
                harvestingRobots = listOf(bp[Material.Ore]!!),
                resources = emptyMap(),
                minutesLeft = 32
            )
            val geodeOpened = resources[Material.Geode] ?: 0
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed geodeOpened
        }.reduce { acc, i -> acc * i }
    }

    measureTimeMillis {
//        val part1 = part1()
    val part2 = part2()
//        println("part1 = $part1")
    println("part2 = $part2")
    }.also { it.print() }
}