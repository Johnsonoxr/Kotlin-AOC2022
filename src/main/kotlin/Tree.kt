class Tree {

    class Node(val name: String) {
        var note: String? = null
    }

    private val branchMap = mutableMapOf<Node, MutableList<Node>>()

    val root = Node("root")

    var node: Node = root

    fun branches(): List<Node>? {
        return branchMap[node]
    }

    fun addBranches(vararg names: String) {
        branchMap.putIfAbsent(node, mutableListOf())
        branchMap[node]?.addAll(names.map { Node(it) })
    }

    fun checkout(branchName: String) {
        node = branchMap[node]!!.first { it.name == branchName }
    }

    fun toParent(): Boolean {
        node = branchMap.filterValues { node in it }.keys.firstOrNull() ?: return false
        return true
    }
}