import java.io.File
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
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

    fun loadData() = File("src/main/resources/Day19.txt").readLines()

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

    fun bestShot(
        blueprints: Map<Material, Blueprint>,
        rpm: MaterialResources,
        resources: MaterialResources,
        minutes: Int
    ): Int? {

        var geodeCrack = resources[Material.Geode]

        if (minutes <= 0) {
            return geodeCrack
        }

        geodeCrack += rpm[Material.Geode] * minutes

        if (minutes <= 1) {
            return geodeCrack
        }

        when (minutes) {
            in 2..3 -> {
                val minuteToWait = minutesToBuildRobot(blueprints[Material.Geode]!!, resources, rpm)
                if (minuteToWait != null) {
                    geodeCrack += min(0, minutes - minuteToWait)
                }
            }

            4 -> {
                val minuteToWait = minutesToBuildRobot(blueprints[Material.Geode]!!, resources, rpm) ?: return geodeCrack // waste of time, early stop
                if (minutes - minuteToWait >= 1) {
                    return bestShot(
                        blueprints = blueprints,
                        rpm = rpm + MaterialResources(mapOf(Material.Geode to 1)),
                        resources = resources + rpm * minuteToWait - blueprints[Material.Geode]!!.costs,
                        minutes = minutes - minuteToWait
                    )
                } else {
                    val (lackedMaterial, lackAmount) = (blueprints[Material.Geode]!!.costs - resources).m.maxBy { it.value }
                    if (lackedMaterial !in rpm) {
                        return geodeCrack    //  waste of time
                    }
                    val mToWait = ceil(lackAmount.toDouble() / rpm[lackedMaterial]).toInt()
                    if (minutes - mToWait >= 3) {
                        return bestShot(
                            blueprints = blueprints,
                            rpm = rpm + MaterialResources(mapOf(lackedMaterial to 1)),
                            resources = resources + rpm * mToWait,
                            minutes = minutes - mToWait
                        )
                    }
                }
            }

            in 5..8 -> {
                return if (Material.Obsidian !in rpm) {
                    // wast of time, early stop
                    0
                } else {
                    null
                }
            }

            else -> return null
        }

        return geodeCrack
    }

    val executors = Executors.newCachedThreadPool()

    fun greedySearch(
        blueprints: Map<Material, Blueprint>,
        harvestingRobots: List<Robot>,
        resources: MaterialResources,
        minutes: Int,
        recording: List<Triple<Int, MaterialResources, Robot>>,
        bestShotRecord: Array<Int>
    ): Pair<List<Robot>, MaterialResources> {

        "Current robots: $harvestingRobots".logv()
        "Current resources: $resources".logv()

        if (harvestingRobots.size == 9) {
            "Searching robots ${harvestingRobots.joinToString("-") { it.harvesting.name }}".logd()
        }

        val resourcesPerMinute = MaterialResources(harvestingRobots.map { it.harvesting }.groupingBy { it }.eachCount())
        var bestRobots = harvestingRobots
        var bestResources = resources + resourcesPerMinute * minutes

        val bestShot = bestShot(blueprints, resourcesPerMinute, resources, minutes)
        if (bestShot != null && bestShot < bestShotRecord[0]) {
            return Pair(bestRobots, bestResources)
        }

        if (minutes > 1) {
            val runnableList = mutableListOf<Runnable>()
            blueprints.values.forEach { blueprint ->
                runnableList.add(Runnable {

                    val minutesWaitForBuild = minutesToBuildRobot(blueprint, resources, resourcesPerMinute) ?: return@Runnable

                    if (minutesWaitForBuild > minutes) {
                        return@Runnable
                    }

                    val robot = Robot(blueprint.robotType)
                    val resLeft = resources + resourcesPerMinute * minutesWaitForBuild - blueprint.costs
                    val minutesLeft = minutes - minutesWaitForBuild

                    val (newRobots, newResource) = greedySearch(
                        blueprints = blueprints,
                        harvestingRobots = harvestingRobots.toMutableList().apply { add(robot) },
                        resources = resLeft,
                        minutes = minutesLeft,
                        recording = recording.toMutableList().apply { add(Triple(minutesLeft, resLeft, robot)) },
                        bestShotRecord = bestShotRecord
                    )

                    if (bestResources[Material.Geode] < newResource[Material.Geode]) {
                        bestResources = newResource
                        bestRobots = newRobots
                    }

                })
            }

            if (harvestingRobots.size in (8..10)) {
                val futures = runnableList.map { executors.submit(it) }
                futures.forEach { it.get() }
            } else {
                runnableList.forEach { it.run() }
            }
        }

        synchronized(bestShotRecord) {
            if (bestShotRecord[0] < bestResources[Material.Geode]) {
                "$bestResources with ${bestRobots.joinToString("-") { it.harvesting.name }}".logi()
            }
            bestShotRecord[0] = max(bestShotRecord[0], bestResources[Material.Geode])
        }

        return Pair(bestRobots, bestResources)
    }

    fun part1(): Int {
        myLogLevel = 2

        return loadBlueprintsList().mapIndexed { idx, bps ->
            val bpIdx = idx + 1

            val robotList = listOf(Robot(Material.Ore))
            val res = MaterialResources()
            val minutesLeft = 24

            val (robots, resources) = greedySearch(
                blueprints = bps,
                harvestingRobots = robotList,
                resources = res,
                minutes = minutesLeft,
                recording = listOf(),
                bestShotRecord = arrayOf(0)
            )
            val geodeOpened = resources[Material.Geode]
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed bpIdx * geodeOpened
        }.reduce { acc, i -> acc + i }
    }

    fun part2(): Int {
        myLogLevel = 1

        val robotList = listOf(Robot(Material.Ore))
        val res = MaterialResources()
        val minutesLeft = 32

        return loadBlueprintsList().take(3).mapIndexed { idx, bps ->
            val bpIdx = idx + 1
            val (robots, resources) = greedySearch(
                blueprints = bps,
                harvestingRobots = robotList,
                resources = res,
                minutes = minutesLeft,
                recording = listOf(),
                bestShotRecord = arrayOf(0)
            )
            val geodeOpened = resources[Material.Geode]
            "Blueprint #$bpIdx gets $geodeOpened geode opened with robots ${robots.joinToString(separator = "-") { it.harvesting.name }}".logi()
            return@mapIndexed geodeOpened
        }.reduce { acc, i -> acc * i }
    }

    measureTimeMillis {
//        val part1 = part1()
        val part2 = part2()
//        println("part1 = $part1")
        println("part2 = $part2")

        executors.shutdownNow()
    }.also { it.print() }
}