import java.io.File

class MyFile(val path: String, val size: Int?) {
    val isDir: Boolean
        get() = size == null

    val name: String
        get() = path.substring(path.lastIndexOf("/") + 1)

    val parentPath: String?
        get() {
            val segments = path.split("/")
            if (segments.size > 1) {
                return segments.toMutableList().apply { removeLast() }.joinToString("/")
            }
            return null
        }
}

fun main() {
    fun loadData() = File("src/main/resources/Day07.txt").readLines()

    fun loadAnalyzedFileStructure(): Set<MyFile> {
        val root = MyFile(
            path = "",
            size = null,
        )
        var currentDir = root

        val fileSet = mutableSetOf<MyFile>()
        fileSet.add(root)

        fun handleCmd(cmd: String) {
            if (!cmd.startsWith("cd")) return

            val dirName = cmd.substring(3)

            currentDir = when (dirName) {
                ".." -> fileSet.find { it.path == currentDir.parentPath }!!
                "/" -> root
                else -> {
                    val path = "${currentDir.path}/$dirName"
                    var dirFile = fileSet.find { it.path == path }
                    if (dirFile == null) {
                        dirFile = MyFile(
                            path = path,
                            size = null
                        )
                        fileSet.add(dirFile)
                    }
                    dirFile
                }
            }
        }

        fun addFileToCurrentDir(fileDescription: String) {
            val dirOrSize = fileDescription.split(" ")[0]
            val name = fileDescription.split(" ")[1]
            val path = "${currentDir.path}/$name"

            if (fileSet.any { it.path == path }) {
                return
            }

            val file = when (dirOrSize) {
                "dir" -> MyFile(
                    path = path,
                    size = null
                )

                else -> MyFile(
                    path = path,
                    size = dirOrSize.toInt()
                )
            }
            fileSet.add(file)
        }

        val cmdLines = loadData()
        cmdLines.forEach { cmd ->
            when {
                cmd.startsWith("$") -> handleCmd(cmd.substring(2))
                else -> addFileToCurrentDir(cmd)
            }
        }
        return fileSet
    }

    fun part1(): Int {
        val fileSet = loadAnalyzedFileStructure()

        fun MyFile.totalSize(): Int {
            return if (isDir) fileSet.filter { it.parentPath == this.path }.sumOf { it.totalSize() } else size!!
        }

        fileSet.toList().sortedBy { it.path }.forEach { println("${it.path}  ${it.totalSize()}") }

        return fileSet.filter { it.isDir }.map { it.totalSize() }.filter { it <= 100_000 }.sum()
    }

    fun part2(): Int {
        val fileSet = loadAnalyzedFileStructure()

        fun MyFile.totalSize(): Int {
            return if (isDir) fileSet.filter { it.parentPath == this.path }.sumOf { it.totalSize() } else size!!
        }

        val diskUsed = fileSet.filter { !it.isDir }.sumOf { it.size!! }
        val diskToFreeUp = diskUsed - 40_000_000

        return fileSet.filter { it.isDir }.map { it.totalSize() }.filter { it >= diskToFreeUp }.min()
    }

    println("part1 = ${part1()}")
    println("part2 = ${part2()}")
}