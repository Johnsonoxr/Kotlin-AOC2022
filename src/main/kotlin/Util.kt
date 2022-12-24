fun Any.log() {
    println(this)
}

fun Any.print() {
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