import util.then

inline fun enabledLog(function: () -> Unit) {
    if (MainKt.enabledLog) {
        function()
    }
}

class MainKt {

    companion object {
        var enabledLog = false
        fun myLog(s: String) {
            enabledLog.then { println(s) }
        }

        inline fun log(s: () -> String) {
            enabledLog.then { println(s()) }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            enabledLog = true
            Runner.main(Array(0) { "" })
        }
    }
}
