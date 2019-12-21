package sim

import strats.StrategyAdvCombined

class SimScore(var score: Double, var simulator: Simulator, var strat: StrategyAdvCombined) {
    var potentialWallsPenalty: Double = 0.0
    var myHealthBonus: Int = 0
    var createdAtTick: Int = 0
    
    override fun toString(): String {
        return "SimScore(score=$score, strat=$strat, potentialWallsPenalty=$potentialWallsPenalty, myHealthBonus=$myHealthBonus)"
    }
}
