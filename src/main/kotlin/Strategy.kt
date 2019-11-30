import model.Game
import model.Unit
import model.UnitAction

interface Strategy {

    fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction

}
