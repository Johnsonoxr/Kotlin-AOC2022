import java.io.File
import kotlin.system.measureTimeMillis

fun main() {

    abstract class Monkey {
        abstract var number: Long?
    }

    class NumberMonkey(override var number: Long?) : Monkey()

    class MathMonkey(
        val child1: String,
        val child2: String,
        val operator: String,
        val monkeyMap: Map<String, Monkey>
    ) : Monkey() {
        override var number: Long? = null
            get() {
                val n1 = monkeyMap[child1]!!.number ?: return null
                val n2 = monkeyMap[child2]!!.number ?: return null
                return when (operator) {
                    "+" -> n1 + n2
                    "-" -> n1 - n2
                    "*" -> n1 * n2
                    "/" -> n1 / n2
                    else -> throw IllegalArgumentException("Unknown operation")
                }
            }
            set(value) {
                field = value
                if (value == null) return

                val c1 = monkeyMap[child1]!!
                val c2 = monkeyMap[child2]!!
                if (c1.number == null) {
                    when (operator) {
                        "+" -> c1.number = value - c2.number!!
                        "-" -> c1.number = value + c2.number!!
                        "*" -> c1.number = value / c2.number!!
                        "/" -> c1.number = value * c2.number!!
                    }
                } else if (c2.number == null) {
                    when (operator) {
                        "+" -> c2.number = value - c1.number!!
                        "-" -> c2.number = c1.number!! - value
                        "*" -> c2.number = value / c1.number!!
                        "/" -> c2.number = c1.number!! / value
                    }
                }
            }
    }

    fun loadData() = File("src/main/resources/Day21.txt").readLines()

    fun loadMonkeys(): Map<String, Monkey> {
        val monkeyMap = mutableMapOf<String, Monkey>()

        val regex = "[a-z]+|[-+*/]|[0-9]+".toRegex()
        loadData().forEach { line ->
            val s = regex.findAll(line).map { it.groupValues[0] }.toList()
            val monkey = when (s.size) {
                2 -> NumberMonkey(s[1].toLong())
                else -> MathMonkey(s[1], s[3], s[2], monkeyMap)
            }

            monkeyMap[s[0]] = monkey
        }
        return monkeyMap
    }

    fun part1(): Long {
        return loadMonkeys()["root"]?.number ?: throw RuntimeException("cant be")
    }

    fun part2(): Long {
        val monkeys = loadMonkeys()
        val rootMonkey = monkeys["root"] as MathMonkey
        val human = monkeys["humn"] as NumberMonkey
        human.number = null

        val c1 = monkeys[rootMonkey.child1]!!
        val c2 = monkeys[rootMonkey.child2]!!

        c1.number?.also { n ->
            c2.number = n
            return human.number ?: 101010101010L
        }
        c2.number?.also { n ->
            c1.number = n
            return human.number ?: 101010101010L
        }

        throw RuntimeException("Not reachable here.")
    }

    measureTimeMillis {
        val part1 = part1()
        val part2 = part2()
        println("part1 = $part1")
        println("part2 = $part2")
    }.also { "$it milliseconds.".print() }
}