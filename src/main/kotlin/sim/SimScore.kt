package sim

import strats.StrategyAdvCombined

class SimScore(var score: Double, var simulator: Simulator, var strat: StrategyAdvCombined) {
    var myHealthBonus: Int = 0
    var createdAtTick: Int = 0

    override fun toString(): String {
        return "sim.EvalAndSim(score=$score, simulator=$simulator, strat=$strat)"
    }
}
