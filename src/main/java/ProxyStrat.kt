import model.Game
import model.Unit
import model.UnitAction

class ProxyStrat(val delegate: StrategyAdvCombined) : Strategy {
    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        return delegate.getAction(unit, game, debug)
    }
}
