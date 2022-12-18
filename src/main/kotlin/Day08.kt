import java.io.File

class Forest(val treeMap: List<List<Int>>) {
    inner class Tree(val x: Int, val y: Int) {

        val height: Int
            get() = treeMap[y][x]

        fun lookLeft(): List<Int> {
            return if (x > 0) treeMap[y].subList(0, x).reversed() else emptyList()
        }

        fun lookRight(): List<Int> {
            return if (x < treeMap[y].size - 1) treeMap[y].subList(x + 1, treeMap[y].size) else emptyList()
        }

        fun lookUp(): List<Int> {
            return if (y > 0) treeMap.map { it[x] }.subList(0, y).reversed() else emptyList()
        }

        fun lookDown(): List<Int> {
            return if (y < treeMap.size) treeMap.map { it[x] }.subList(y + 1, treeMap.size) else emptyList()
        }
    }

    fun trees() = treeMap.flatMapIndexed { x, row -> row.indices.map { y -> Tree(x, y) } }
}

fun main() {
    fun loadData() = File("src/main/resources/Day08.txt").readLines()

    fun loadForest(): Forest {
        return Forest(loadData().map { it.map { c -> c.digitToInt() } })
    }

    fun part1(): Int {
        val forest = loadForest()

        val visibleTrees = forest.trees().filter { tree ->
            when {
                (tree.lookLeft().maxOrNull() ?: -1) < tree.height -> true
                (tree.lookRight().maxOrNull() ?: -1) < tree.height -> true
                (tree.lookUp().maxOrNull() ?: -1) < tree.height -> true
                (tree.lookDown().maxOrNull() ?: -1) < tree.height -> true
                else -> false
            }
        }
        return visibleTrees.count()
    }

    fun part2(): Int {
        val forest = loadForest()

        fun visionDistance(tree: Forest.Tree, otherTreeHeights: List<Int>): Int {
            val blockingDistance = otherTreeHeights.indexOfFirst { it >= tree.height }
            return if (blockingDistance == -1) otherTreeHeights.size else (blockingDistance + 1)
        }

        val scenicScores = forest.trees().map { tree ->
            val leftVisionDistance = visionDistance(tree, tree.lookLeft())
            val rightVisionDistance = visionDistance(tree, tree.lookRight())
            val upVisionDistance = visionDistance(tree, tree.lookUp())
            val downVisionDistance = visionDistance(tree, tree.lookDown())
            leftVisionDistance * rightVisionDistance * upVisionDistance * downVisionDistance
        }
        return scenicScores.max()
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}