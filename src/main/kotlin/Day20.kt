import java.io.File
import kotlin.system.measureTimeMillis

fun main() {

    fun loadData() = File("src/main/resources/Day20.txt").readLines()

    fun loadEncryptedNumbers() = loadData().map { it.toInt() }

    fun decrypt(data: List<Long>, decryptOrder: List<Long>): List<Long> {
        val mutableNumbers = data.toMutableList()

        data.logv()

        val cycleSize = data.size - 1

        for (number in decryptOrder) {
            val prevIdx = mutableNumbers.indexOf(number)
            mutableNumbers.removeAt(prevIdx)
            val newIndex = ((prevIdx + number) % cycleSize + cycleSize) % cycleSize
            mutableNumbers.add(newIndex.toInt(), number)

            mutableNumbers.logv()
        }

        return mutableNumbers
    }

    fun part1(): Int {
        myLogLevel = 1

        val data = loadEncryptedNumbers().map { it.toLong() }
        val decryptedData = decrypt(data, data)
        val idxOfZero = decryptedData.indexOf(0)
        val coordinates = listOf(1000, 2000, 3000).map { offset ->
            decryptedData[((idxOfZero + offset) % decryptedData.size + decryptedData.size) % decryptedData.size]
        }
        coordinates.logi()
        return coordinates.sum().toInt()
    }

    fun part2(): Long {
        myLogLevel = 2

        val data = loadEncryptedNumbers().map { it.toLong() * 811589153L }
        var decryptedData = data

        decryptedData.logd()

        repeat(times = 10) {
            decryptedData = decrypt(decryptedData, data)
            decryptedData.logd()
        }

        val idxOfZero = decryptedData.indexOf(0)
        val coordinates = listOf(1000, 2000, 3000).map { offset ->
            decryptedData[((idxOfZero + offset) % decryptedData.size + decryptedData.size) % decryptedData.size]
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