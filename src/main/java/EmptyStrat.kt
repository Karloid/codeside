import model.Game
import model.Unit
import model.UnitAction

class EmptyStrat(myStrategy: MyStrategy) : Strategy {
    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        return UnitAction()
    }

}
