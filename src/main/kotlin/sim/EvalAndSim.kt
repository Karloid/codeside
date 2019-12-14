package sim

import sim.Simulator
import strats.Strategy

class EvalAndSim(var score: Double, var simulator: Simulator, var strat: Strategy) {
    var createdAtTick: Int = 0

    override fun toString(): String {
        return "sim.EvalAndSim(score=$score, simulator=$simulator, strat=$strat)"
    }
}
