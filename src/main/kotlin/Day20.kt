import java.io.File

fun main() {

    fun loadData() = File("src/main/resources/Day20t.txt").readLines()

    fun loadEncryptedNumbers() = loadData().map { it.toInt() }

    fun decrypt(numbers: List<Int>) {
        val numberOrderPairs = numbers.zip(numbers.indices).toMutableList()

        for (i in numbers.indices) {
            val pair = numberOrderPairs.find { it.second == i }!!
            val prevIdx = numberOrderPairs.indexOf(pair)
            numberOrderPairs.removeAt(prevIdx)
            val newIndex = ((pair.first % numbers.size) + numbers.size + prevIdx) % numbers.size
            numberOrderPairs.add(newIndex, pair)
        }

        val decryptedNumbers = numberOrderPairs.map { it.first }
        decryptedNumbers.print()
    }

    decrypt(loadEncryptedNumbers())

//    val part1 = part1()
//    val part2 = part2()
//    println("part1 = $part1")
//    println("part2 = $part2")
}