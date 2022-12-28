fun Any.log() {
    println(this)
}

fun Any?.print() {
    println(this)
}

var myLogLevel = 0

fun Any.logv() {
    if (myLogLevel <= 0) {
        println(this)
    }
}

fun Any.logd() {
    if (myLogLevel <= 1) {
        println(this)
    }
}

fun Any.logi() {
    if (myLogLevel <= 2) {
        println(this)
    }
}

enum class Dir(val dx: Int, val dy: Int) {
    LEFT(-1, 0),
    UP(0, -1),
    RIGHT(1, 0),
    DOWN(0, 1);

    fun turn(turn: Turn): Dir = when (turn) {
        Turn.L -> Dir.values()[(this.ordinal + 3) % 4]
        Turn.R -> Dir.values()[(this.ordinal + 1) % 4]
    }

    fun plot() = when (this) {
        LEFT -> "<"
        UP -> "^"
        RIGHT -> ">"
        DOWN -> "v"
    }

    fun isHorizontal() = this == LEFT || this == RIGHT

    fun isVertical() = !isHorizontal()

    fun reverseDir() = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        UP -> DOWN
        DOWN -> UP
    }
}

enum class Turn {
    L,
    R;
}
