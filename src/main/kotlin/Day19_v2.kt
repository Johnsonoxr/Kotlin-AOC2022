import java.io.File
import kotlin.math.ceil
import kotlin.system.measureTimeMillis


fun main() {

    var robotId = 0

    class MaterialResources(val m: Map<Material, Int>) {
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

    fun loadBlueprints(): List<Map<Material, Blueprint>> {
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

    loadBlueprints().forEach { it.logi() }

    fun whoShouldBeFired(recording: List<Triple<Int, MaterialResources, Robot>>): Robot? {
        recording.forEachIndexed { i, (minuteLeftWhenCreated, resWhenCreated, robot) ->
            val material = robot.harvesting
            recording.subList(i + 1, recording.size).forEach { (minutesLeft, res, _) ->
                if (minuteLeftWhenCreated - minutesLeft > res.m[material]!! - (resWhenCreated.m[material] ?: 0)) {
                    return@forEachIndexed
                }
            }

            if (robot.harvesting != Material.Ore) {
                return@forEachIndexed
            }
            "Zombie found. $robot".logv()
            return robot
        }
        return null
    }

    fun harvest(
        blueprint: Map<Material, Blueprint>,
        recording: List<Triple<Int, MaterialResources, Robot>>,
        employeeToBeFire: Array<Robot?>,
        harvestingRobots: List<Robot>,
        resources: MaterialResources,
        minutesLeft: Int
    ): Pair<List<Robot>, MaterialResources> {

        if (employeeToBeFire[0] != null && employeeToBeFire[0] in harvestingRobots) {
            return Pair(harvestingRobots, resources)
        }

        if (minutesLeft < 1) {
            employeeToBeFire[0] = whoShouldBeFired(recording)
            return Pair(harvestingRobots, resources)
        }

        "Current robots: $harvestingRobots".logv()
        "Current resources: $resources".logv()

        var bestRobots = harvestingRobots
        var bestResources = resources + MaterialResources(harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount()) * minutesLeft

        blueprint.values.sortedBy { it.robotType.ordinal }.reversed().forEach { bp ->
            val resourcesPerMinute = MaterialResources(harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount())

            val resAfterBuild = resources - bp.costs

            val minutesWait = if (resAfterBuild.m.all { it.value >= 0 }) {
                1
            } else {
                val lackedResources = MaterialResources(resAfterBuild.m.filter { it.value < 0 })
                if (lackedResources.m.any { it.key !in resourcesPerMinute.m }) {
                    return@forEach
                } else {
                    val minToWait = 1 + lackedResources.m.maxOf { (m, n) -> ceil(-n.toDouble() / resourcesPerMinute.m[m]!!).toInt() }
                    minToWait
                }
            }

            if (minutesWait >= minutesLeft) {
                employeeToBeFire[0] = whoShouldBeFired(recording)
                return@forEach
            }

            val resAfterWait = resAfterBuild + resourcesPerMinute * minutesWait
            val robot = Robot(bp.robotType)

            val (newRobots, newResource) = harvest(
                blueprint = blueprint,
                recording = recording.toMutableList().apply { add(Triple(minutesLeft - minutesWait, resAfterWait, robot)) },
                employeeToBeFire = employeeToBeFire,
                harvestingRobots = harvestingRobots.toMutableList().apply { add(robot) },
                resources = resAfterWait,
                minutesLeft = minutesLeft - minutesWait
            )

            if (bestResources.m.getOrDefault(Material.Geode, defaultValue = 0) < newResource.m.getOrDefault(Material.Geode, defaultValue = 0)) {
                bestResources = newResource
                bestRobots = newRobots
            }
        }

        return Pair(bestRobots, bestResources)
    }

    fun part1(): Int {
        myLogLevel = 2

        return loadBlueprints().mapIndexed { idx, bp ->
            val bpIdx = idx + 1
            val (robots, resources) = harvest(
                blueprint = bp,
                recording = listOf(),
                employeeToBeFire = arrayOf(null),
                harvestingRobots = listOf(Robot(Material.Ore)),
                resources = MaterialResources(emptyMap()),
                minutesLeft = 24
            )
            val geodeOpened = resources.m[Material.Geode] ?: 0
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed bpIdx * geodeOpened
        }.reduce { acc, i -> acc + i }
    }

    fun part2(): Int {
        myLogLevel = 2

        return loadBlueprints().take(3).mapIndexed { idx, bp ->
            val bpIdx = idx + 1
            val (robots, resources) = harvest(
                blueprint = bp,
                recording = listOf(),
                employeeToBeFire = arrayOf(null),
                harvestingRobots = listOf(Robot(Material.Ore)),
                resources = MaterialResources(emptyMap()),
                minutesLeft = 32
            )
            val geodeOpened = resources.m[Material.Geode] ?: 0
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed geodeOpened
        }.reduce { acc, i -> acc * i }
    }

    measureTimeMillis {
        val part1 = part1()
//        val part2 = part2()
        println("part1 = $part1")
//        println("part2 = $part2")
    }.also { it.print() }
}