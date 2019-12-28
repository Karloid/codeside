package sim

import strats.StrategyAdvCombined

class SimScore(var score: Double, var simulator: Simulator, var strat: StrategyAdvCombined) {
    var pathToHealPenalty: Double = 0.0
    var pathToGunPenalty: Double = 0.0
    var potentialWallsPenalty: Double = 0.0
    var myHealthBonus: Int = 0
    var enHealthPenalty: Int = 0
    var createdAtTick: Int = 0
    override fun toString(): String {
        return "SimScore(score=$score, strat=$strat, pathToGunPenalty=$pathToGunPenalty, potentialWallsPenalty=$potentialWallsPenalty," +
                " myHealthBonus=$myHealthBonus" +
                " enHealthPenalty=$enHealthPenalty" +
                " pathToHealPenalty=$pathToHealPenalty" +
                ")"
    }

}
