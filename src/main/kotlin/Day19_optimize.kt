import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.system.measureTimeMillis

interface SearchResult : Callable<Pair<List<Material>, Int>>

//  Optimization inspired by https://www.youtube.com/watch?v=H3PSODv4nf0&ab_channel=hyper-neutrino
fun main() {

    class Resources(val mat: Map<Material, Int> = emptyMap()) {
        operator fun plus(res: Resources): Resources {
            return Resources(mat.toMutableMap().apply {
                res.mat.forEach { (m, n) -> this[m] = this.getOrDefault(m, defaultValue = 0) + n }
            })
        }

        operator fun plus(material: Material): Resources {
            return Resources(mat.toMutableMap().apply { put(material, getOrDefault(material, defaultValue = 0) + 1) })
        }

        operator fun minus(res: Resources): Resources {
            return Resources(mat.toMutableMap().apply {
                res.mat.forEach { (m, n) -> this[m] = this.getOrDefault(m, defaultValue = 0) - n }
            })
        }

        operator fun times(n: Int): Resources {
            return Resources(mat.mapValues { it.value * n })
        }

        operator fun div(resources: Resources): Int? {
            if (mat.any { (m, n) -> n != 0 && resources[m] == 0 }) {
                return null
            }
            return ceil(resources.mat.maxOf { (m, n) -> this.mat.getOrDefault(m, defaultValue = 0).toDouble() / n }).toInt()
        }

        operator fun get(material: Material): Int {
            return this.mat.getOrDefault(material, defaultValue = 0)
        }

        operator fun contains(material: Material): Boolean {
            return this.mat.getOrDefault(material, defaultValue = 0) > 0
        }

        override fun toString(): String {
            return "Res(${mat.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
        }

        fun toKey(): Long {
            return get(Material.Ore) + 1000L * get(Material.Clay) + 1000_000L * get(Material.Obsidian) + 1000_000_000L * get(Material.Geode)
        }
    }

    data class Blueprint(val robotType: Material, val costs: Resources)

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
                blueprint[produce] = Blueprint(produce, Resources(costs))
            }
        }
        return blueprints
    }

    loadBlueprintsList().forEach { it.logi() }

    fun minutesToBuildRobot(
        blueprint: Blueprint,
        resources: Resources,
        rpm: Resources
    ): Int? {
        val resLacked = blueprint.costs - resources

        return if (resLacked.mat.all { it.value <= 0 }) {
            1
        } else {
            val minutes = resLacked / rpm
            if (minutes != null) {
                minutes + 1
            } else {
                null
            }
        }
    }

    fun predictBestGeodes(
        blueprints: Map<Material, Blueprint>,
        rpm: Resources,
        res: Resources,
        time: Int
    ): Int? {
        return when (time) {
            0 -> res[Material.Geode]
            1 -> res[Material.Geode] + rpm[Material.Geode]
            in 2..3 -> {
                val minuteToWait = minutesToBuildRobot(blueprints[Material.Geode]!!, res, rpm)
                res[Material.Geode] + rpm[Material.Geode] * time + max(0, time - (minuteToWait ?: Int.MAX_VALUE))
            }

            else -> null
        }
    }

    fun calcMaxMaterialsSpentInOneMinute(blueprints: Map<Material, Blueprint>): Resources {
        val map = mutableMapOf<Material, Int>()
        blueprints.values.forEach { blueprint ->
            blueprint.costs.mat.forEach { (m, n) ->
                if (map.getOrDefault(m, defaultValue = 0) < n) {
                    map[m] = n
                }
            }
        }
        map[Material.Geode] = 10000 //  Geodes should be as mush as possible.
        return Resources(map)
    }

    val executor = Executors.newCachedThreadPool()

    fun greedySearch(
        blueprints: Map<Material, Blueprint>,
        robots: List<Material>,
        resources: Resources,
        time: Int,
        cacheOfGeodes: MutableMap<String, Int>,
        bestGeodesForNow: IntArray,
        maxMaterialsSpentPerMinute: Resources,
        threadingLevel: Int
    ): Pair<List<Material>, Int> {

        "Current robots: $robots".logv()
        "Current resources: $resources".logv()

        val rpm = Resources(robots.groupingBy { it }.eachCount())

        val resultKey = "$time-${rpm.toKey()}-${resources.toKey()}"
        val geodesInCache = cacheOfGeodes[resultKey]
        if (geodesInCache != null) {
            return Pair(robots, geodesInCache)
        }

        val geodesIfDoNothing = resources[Material.Geode] + rpm[Material.Geode] * time
        val bestGeodesByPredict = predictBestGeodes(blueprints, rpm, resources, time)
        if (bestGeodesByPredict != null && bestGeodesByPredict < bestGeodesForNow[0]) {
            return Pair(robots, geodesIfDoNothing)
        }

        val threading = threadingLevel < 3 && time < 15

        val tasks = blueprints.values.filter { rpm[it.robotType] < maxMaterialsSpentPerMinute[it.robotType] }.map { blueprint ->
            object : SearchResult {
                override fun call(): Pair<List<Material>, Int> {
                    val minutesWaitForBuild = minutesToBuildRobot(blueprint, resources, rpm) ?: return Pair(robots, geodesIfDoNothing)

                    if (minutesWaitForBuild > time) {
                        return Pair(robots, geodesIfDoNothing)
                    }

                    val resLeft = resources + rpm * minutesWaitForBuild - blueprint.costs
                    val nextRobots = robots.toMutableList().apply { add(blueprint.robotType) }
                    val minutesLeft = time - minutesWaitForBuild

                    return greedySearch(
                        blueprints = blueprints,
                        robots = nextRobots,
                        resources = resLeft,
                        time = minutesLeft,
                        cacheOfGeodes = cacheOfGeodes,
                        bestGeodesForNow = bestGeodesForNow,
                        maxMaterialsSpentPerMinute = maxMaterialsSpentPerMinute,
                        threadingLevel = threadingLevel + if (threading) 1 else 0
                    )
                }
            }
        }

        val results = if (threading) {
            tasks.map { executor.submit(it) }.map { it.get() }
        } else {
            tasks.map { it.call() }
        }

        val bestResult = results.maxByOrNull { it.second } ?: return Pair(robots, geodesIfDoNothing)

        cacheOfGeodes[resultKey] = bestResult.second

        if (bestGeodesForNow[0] < bestResult.second) {
            "Crack ${bestResult.second} geodes by ${bestResult.first.joinToString("-") { it.name }}".logi()
        }
        bestGeodesForNow[0] = max(bestGeodesForNow[0], bestResult.second)

        return bestResult
    }

    fun part1(): Int {
        myLogLevel = 2

        return loadBlueprintsList().mapIndexed { idx, bps ->
            val bpIdx = idx + 1

            val result = greedySearch(
                blueprints = bps,
                robots = listOf(Material.Ore),
                resources = Resources(),
                time = 24,
                cacheOfGeodes = mutableMapOf(),
                bestGeodesForNow = intArrayOf(0),
                maxMaterialsSpentPerMinute = calcMaxMaterialsSpentInOneMinute(bps),
                threadingLevel = 0
            )
            "Blueprint #$bpIdx gets ${result.second} geode opened with robots ${result.first.joinToString(separator = "-") { it.name }}".logi()
            return@mapIndexed bpIdx * result.second
        }.reduce { acc, i -> acc + i }
    }

    fun part2(): Int {
        myLogLevel = 2

        return loadBlueprintsList().take(3).mapIndexed { idx, bps ->
            val bpIdx = idx + 1

            val result = greedySearch(
                blueprints = bps,
                robots = listOf(Material.Ore),
                resources = Resources(),
                time = 32,
                cacheOfGeodes = mutableMapOf(),
                bestGeodesForNow = intArrayOf(0),
                maxMaterialsSpentPerMinute = calcMaxMaterialsSpentInOneMinute(bps),
                threadingLevel = 0
            )
            "Blueprint #$bpIdx gets ${result.second} geode opened with robots ${result.first.joinToString(separator = "-") { it.name }}".logi()
            return@mapIndexed result.second
        }.reduce { acc, i -> acc * i }
    }

    measureTimeMillis {
        val part1 = part1()
        val part2 = part2()
        println("part1 = $part1")
        println("part2 = $part2")

        executor.shutdownNow()
    }.also { it.print() }
}