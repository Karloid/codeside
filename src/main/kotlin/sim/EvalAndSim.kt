package sim

import strats.StrategyAdvCombined

class EvalAndSim(var score: Double, var simulator: Simulator, var strat: StrategyAdvCombined) {
    var createdAtTick: Int = 0

    override fun toString(): String {
        return "sim.EvalAndSim(score=$score, simulator=$simulator, strat=$strat)"
    }
}
