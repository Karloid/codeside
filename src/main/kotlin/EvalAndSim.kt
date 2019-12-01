class EvalAndSim(var score: Double, var simulator: Simulator, var strat: Strategy) {
    var createdAtTick: Int = 0

    override fun toString(): String {
        return "EvalAndSim(score=$score, simulator=$simulator, strat=$strat)"
    }
}
