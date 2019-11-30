class MainKt {
    companion object {
        var enabledLog = true
        fun myLog(s: String) {
            enabledLog.then { println(s) }
        }

        inline fun myLog(s: () -> String) {
            enabledLog.then { println(s()) }
        }
    }
}
