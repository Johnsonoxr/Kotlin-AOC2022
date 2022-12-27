import java.io.File
import kotlin.system.measureTimeMillis

fun main() {

    fun loadData() = File("src/main/resources/Day20.txt").readLines()

    fun loadEncryptedNumbers() = loadData().map { it.toInt() }

    fun decrypt(data: List<Pair<Long, Int>>): List<Pair<Long, Int>> {
        val mutableNumbers = data.toMutableList()
        val sortedData = data.sortedBy { it.second }

        data.logv()

        val cycleSize = data.size - 1

        for (pair in sortedData) {
            val prevIdx = mutableNumbers.indexOf(pair)
            mutableNumbers.removeAt(prevIdx)
            val newIndex = ((prevIdx + pair.first) % cycleSize + cycleSize) % cycleSize
            mutableNumbers.add(newIndex.toInt(), pair)
            mutableNumbers.logv()
        }

        return mutableNumbers
    }

    fun part1(): Int {
        myLogLevel = 1

        val data = loadEncryptedNumbers().map { it.toLong() }
        val decryptedData = decrypt(data.zip(data.indices))
        val decryptedNumbers = decryptedData.map { it.first }

        val idxOfZero = decryptedNumbers.indexOf(0)
        val coordinates = listOf(1000, 2000, 3000).map { offset ->
            decryptedNumbers[((idxOfZero + offset) % decryptedNumbers.size + decryptedNumbers.size) % decryptedNumbers.size]
        }
        coordinates.logi()
        return coordinates.sum().toInt()
    }

    fun part2(): Long {
        myLogLevel = 2

        val data = loadEncryptedNumbers().map { it.toLong() * 811589153L }
        var decryptedData = data.zip(data.indices)
        repeat(times = 10) {
            decryptedData = decrypt(decryptedData)
            decryptedData.logd()
        }
        val decryptedNumbers = decryptedData.map { it.first }

        val idxOfZero = decryptedNumbers.indexOf(0)
        val coordinates = listOf(1000, 2000, 3000).map { offset ->
            decryptedNumbers[((idxOfZero + offset) % decryptedNumbers.size + decryptedNumbers.size) % decryptedNumbers.size]
        }
        coordinates.logi()
        return coordinates.sum()
    }

    measureTimeMillis {
        val part1 = part1()
        val part2 = part2()
        println("part1 = $part1")
        println("part2 = $part2")
    }.also { it.print() }
}