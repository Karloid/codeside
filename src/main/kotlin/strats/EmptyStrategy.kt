package strats

import Debug
import core.MyStrategy
import model.Game
import model.Unit
import model.UnitAction

class EmptyStrategy(myStrategy: MyStrategy) : AbstractStrategy() {
    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        return UnitAction()
    }

}
