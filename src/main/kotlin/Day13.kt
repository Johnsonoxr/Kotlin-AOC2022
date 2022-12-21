import java.io.File

fun main() {
    fun loadData() = File("src/main/resources/Day13.txt").readLines()

    fun loadPairs(): Map<Int, Pair<List<Any>, List<Any>>> {

        fun cvtLineToList(line: String): List<Any> {
            val strList = line.replace("[", "[,").replace("]", ",]").replace(",,", ",").split(",")
            var ptr = 1

            fun innerCvtLineToList(): List<Any> {
                val innerList = mutableListOf<Any>()
                while (ptr < strList.size) {
                    when (val item = strList[ptr++]) {
                        "[" -> innerList.add(innerCvtLineToList())
                        "]" -> break
                        else -> innerList.add(item.toInt())
                    }
                }
                return innerList
            }

            return innerCvtLineToList()
        }

        val rst = loadData().chunked(3).map { (line1, line2, _) ->
            val list1 = cvtLineToList(line1)
            val list2 = cvtLineToList(line2)
            Pair(list1, list2)
        }
        return rst.associateBy { rst.indexOf(it) + 1 }
    }

    fun isValidPair(list1: List<Any>, list2: List<Any>, verboseTab: Int = Int.MIN_VALUE): Boolean? {
        fun String.log() {
            if (verboseTab < 0) {
                return
            }
            println("${Array(verboseTab) { "  " }.joinToString("")}$this")
        }

        "Compare $list1 vs $list2".log()
        var ptr = 0
        while (true) {
            when {
                ptr < list1.size && ptr >= list2.size -> {
                    "  Right side run out of items".log()
                    return false
                }

                ptr >= list1.size && ptr < list2.size -> {
                    "  Left side run out of items".log()
                    return true
                }

                ptr >= list1.size && ptr >= list2.size -> return null
                else -> {
                    val n1 = list1[ptr] as? Int
                    val n2 = list2[ptr] as? Int
                    val l1 = list1[ptr] as? List<Any>
                    val l2 = list2[ptr] as? List<Any>
                    when {
                        n1 != null && n2 != null -> {
                            "  Compare $n1 vs $n2".log()
                            when {
                                n1 > n2 -> {
                                    "  Right side is smaller".log()
                                    return false
                                }

                                n1 < n2 -> {
                                    "  Left side is smaller".log()
                                    return true
                                }
                            }
                        }

                        l1 != null && l2 != null -> {
                            when (isValidPair(l1, l2, verboseTab + 1)) {
                                true -> return true
                                false -> return false
                                else -> {}
                            }
                        }

                        n1 != null && l2 != null -> {
                            when (isValidPair(listOf(n1), l2, verboseTab + 1)) {
                                true -> return true
                                false -> return false
                                else -> {}
                            }
                        }

                        l1 != null && n2 != null -> {
                            when (isValidPair(l1, listOf(n2), verboseTab + 1)) {
                                true -> return true
                                false -> return false
                                else -> {}
                            }
                        }
                    }
                }
            }
            ptr++
        }
    }

    fun part1(): Int {
        val pairList = loadPairs()

        var sum = 0
        pairList.forEach { index, (line1, line2) ->
            "Check #$index: $line1 vs $line2".log()
            val rst = isValidPair(line1, line2, verboseTab = 1) == true

            if (rst) {
                "Valid\n".log()
                sum += index
            } else {
                "Invalid\n".log()
            }
        }

        return sum
    }

    fun part2(): Int {
        val pairList = loadPairs()

        val signalList = pairList.values.flatMap { (l1, l2) -> listOf(l1, l2) }.toMutableList()
        val additional2 = listOf(listOf(2))
        val additional6 = listOf(listOf(6))
        signalList.add(additional2)
        signalList.add(additional6)

        //  bubble sort
        (1 until signalList.size).reversed().forEach { end ->
            (0 until end).forEach { idx ->
                val signal1 = signalList[idx]
                val signal2 = signalList[idx + 1]
                if (isValidPair(signal1, signal2) != true) {
                    signalList[idx] = signal2
                    signalList[idx + 1] = signal1
                }
            }
        }

        signalList.joinToString("\n").log()

        return (signalList.indexOf(additional2) + 1) * (signalList.indexOf(additional6) + 1)
    }

    val part1 = part1()
    val parr2 = part2()
    println("part1 = $part1")
    println("part2 = $parr2")
}