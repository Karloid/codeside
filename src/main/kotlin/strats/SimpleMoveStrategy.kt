package strats

import Debug
import model.Game
import model.Unit
import model.UnitAction

class SimpleMoveStrategy : StrategyAdvCombined {
    override var isReal: Boolean = false

    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {

        return UnitAction()
    }
}
