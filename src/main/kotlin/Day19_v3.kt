import java.io.File
import kotlin.math.ceil
import kotlin.system.measureTimeMillis


fun main() {

    var robotId = 0

    class MaterialResources(val m: Map<Material, Int> = emptyMap()) {
        operator fun plus(res: MaterialResources): MaterialResources {
            return MaterialResources(m.toMutableMap().apply {
                res.m.forEach { (m, n) -> this[m] = this.getOrDefault(m, 0) + n }
            })
        }

        operator fun minus(res: MaterialResources): MaterialResources {
            return MaterialResources(m.toMutableMap().apply {
                res.m.forEach { (m, n) -> this[m] = this.getOrDefault(m, 0) - n }
            })
        }

        operator fun times(n: Int): MaterialResources {
            return MaterialResources(m.mapValues { it.value * n })
        }

        operator fun get(material: Material): Int {
            return this.m.getOrDefault(material, defaultValue = 0)
        }

        operator fun contains(material: Material): Boolean {
            return this.m.getOrDefault(material, defaultValue = 0) > 0
        }

        override fun toString(): String {
            return "Res(${m.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
        }
    }

    data class Robot(val harvesting: Material) {
        val id = robotId++

        override fun toString(): String {
            return "$harvesting-$id"
        }

        override fun hashCode(): Int {
            return id
        }

        override fun equals(other: Any?): Boolean {
            return other is Robot && other.id == id
        }
    }

    data class Blueprint(val robotType: Material, val costs: MaterialResources)

    fun loadData() = File("src/main/resources/Day19t.txt").readLines()

    fun loadBlueprintsList(): List<Map<Material, Blueprint>> {
        val blueprints = mutableListOf<Map<Material, Blueprint>>()
        val blueprintSplitRegex = "Blueprint [0-9]+:".toRegex()
        val materialRetrieveRegex = "(${Material.values().joinToString("|") { it.str }})|[0-9]+".toRegex()

        loadData().joinToString("").split(blueprintSplitRegex).filter { it.isNotEmpty() }.forEach { line ->
            val blueprint = mutableMapOf<Material, Blueprint>()
            blueprints.add(blueprint)

            line.split(".").filter { it.isNotEmpty() }.map { linePerRobot ->
                val words = materialRetrieveRegex.findAll(linePerRobot).map { it.groupValues[0] }.toList()
                val produce = Material.fromStr(words.first())
                val costs = words.subList(1, words.size).chunked(2).associate { (n, material) -> Material.fromStr(material) to n.toInt() }
                blueprint[produce] = Blueprint(produce, MaterialResources(costs))
            }
        }
        return blueprints
    }

    loadBlueprintsList().forEach { it.logi() }

    fun minutesToBuildRobot(
        blueprint: Blueprint,
        resources: MaterialResources,
        resourcesPerMinute: MaterialResources
    ): Int? {
        val resAfterBuild = resources - blueprint.costs
        val lackedResources = resAfterBuild.m.filter { it.value < 0 }
        val minutesWait = when {
            lackedResources.isEmpty() -> 1
            lackedResources.any { resourcesPerMinute[it.key] == 0 } -> null
            else -> 1 + lackedResources.maxOf { (m, n) -> ceil(-n.toDouble() / resourcesPerMinute[m]).toInt() }
        }
        return minutesWait
    }

    fun bestHarvesting(
        blueprints: Map<Material, Blueprint>,
        harvestingRobots: List<Robot>,
        resources: MaterialResources,
        minutesLeft: Int,
    ): Pair<List<Robot>, MaterialResources> {

        if (minutesLeft < 1) {
            return Pair(harvestingRobots, resources)
        }

        "Current robots: $harvestingRobots".logv()
        "Current resources: $resources".logv()

        val resourcesPerMinute = MaterialResources(harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount())

        var bestRobots = harvestingRobots
        var bestResources = resources + resourcesPerMinute * minutesLeft

        blueprints.values.forEach { blueprint ->

            val minutesWaitForBuild = minutesToBuildRobot(blueprint, resources, resourcesPerMinute) ?: return@forEach

            if (minutesWaitForBuild > minutesLeft) {
                return@forEach
            }

            val robot = Robot(blueprint.robotType)
            val resAfterBuild = resources + resourcesPerMinute * minutesWaitForBuild - blueprint.costs

            val (newRobots, newResource) = bestHarvesting(
                blueprints = blueprints,
                harvestingRobots = harvestingRobots.toMutableList().apply { add(robot) },
                resources = resAfterBuild,
                minutesLeft = minutesLeft - minutesWaitForBuild,
            )

            if (bestResources[Material.Geode] < newResource[Material.Geode]) {
                bestResources = newResource
                bestRobots = newRobots
            }
        }

        return Pair(bestRobots, bestResources)
    }

    fun shortTermTest(
        shortTermMinutes: Int,
        blueprints: Map<Material, Blueprint>,
        harvestingRobots: List<Robot>,
        resources: MaterialResources
    ): Blueprint? {

        val evaluation = mutableMapOf<Blueprint, Int>()

        val resourcesPerMinute = MaterialResources(harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount())

        blueprints.values.forEach { blueprint ->
            val minutesWaitForBuild = minutesToBuildRobot(blueprint, resources, resourcesPerMinute) ?: return@forEach

            if (minutesWaitForBuild > shortTermMinutes) {
                return@forEach
            }

            val robot = Robot(blueprint.robotType)
            val resAfterBuild = resources + resourcesPerMinute * minutesWaitForBuild - blueprint.costs

            val (newRobots, newResource) = bestHarvesting(
                blueprints = blueprints,
                harvestingRobots = harvestingRobots.toMutableList().apply { add(robot) },
                resources = resAfterBuild,
                minutesLeft = shortTermMinutes - minutesWaitForBuild,
            )

            evaluation[blueprint] = 100 * newRobots.count { it.harvesting == Material.Geode } +
                    50 * newResource[Material.Geode] +
                    10 * newRobots.count { it.harvesting == Material.Obsidian } +
                    5 * newResource[Material.Obsidian] +
                    2 * newRobots.count { it.harvesting == Material.Clay } +
                    1 * newResource[Material.Clay] +
                    1 * newRobots.count { it.harvesting == Material.Ore }
        }

        "shortTermTest: $evaluation".logd()

        return evaluation.maxByOrNull { it.value }?.key
    }

    fun part1(): Int {
        myLogLevel = 1

        return loadBlueprintsList().take(1).mapIndexed { idx, bps ->
            val bpIdx = idx + 1

            var minutesLeft = 24
            val shortTermTestLength = 10

            var res = MaterialResources()
            var robotList = listOf(Robot(Material.Ore))

//            while (minutesLeft - shortTermTestLength > 0) {
//                val nextBlueprintToChoose = shortTermTest(
//                    blueprints = bps,
//                    harvestingRobots = robotList,
//                    resources = res,
//                    shortTermMinutes = shortTermTestLength
//                )!!
//
//                val resourcesPerMinute = MaterialResources(robotList.map { it.harvesting }.groupingBy { it }.eachCount())
//                val minWait = minutesToBuildRobot(nextBlueprintToChoose, res, resourcesPerMinute)!!
//                minutesLeft -= minWait
//
//                robotList = robotList.toMutableList().apply { add(Robot(nextBlueprintToChoose.robotType)) }
//                res = res + resourcesPerMinute * minWait - nextBlueprintToChoose.costs
//            }

            val (robots, resources) = bestHarvesting(
                blueprints = bps,
                harvestingRobots = robotList,
                resources = res,
                minutesLeft = minutesLeft
            )
            val geodeOpened = resources[Material.Geode]
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed bpIdx * geodeOpened
        }.reduce { acc, i -> acc + i }
    }

//    fun part2(): Int {
//        myLogLevel = 2
//
//        return loadBlueprints().take(3).mapIndexed { idx, bp ->
//            val bpIdx = idx + 1
//            val (robots, resources) = harvest(
//                blueprint = bp,
//                employeeToBeFire = arrayOf(null),
//                harvestingRobots = listOf(Robot(Material.Ore)),
//                resources = MaterialResources(emptyMap()),
//                minutesLeft = 32
//            )
//            val geodeOpened = resources.m[Material.Geode] ?: 0
//            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
//            return@mapIndexed geodeOpened
//        }.reduce { acc, i -> acc * i }
//    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { it.print() }
}