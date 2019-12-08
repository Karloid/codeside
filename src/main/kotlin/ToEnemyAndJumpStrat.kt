import model.Game
import model.Unit
import model.UnitAction

class ToEnemyAndJumpStrat : StrategyAdvCombined {
    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        val act = UnitAction()

        val deltaX =
            me.position.x - game.units.filter { it.playerId != me.playerId }.minBy { it.position.distance(me.position) }!!.position.x

        if (deltaX.compareTo(0) < 0) {
            act.velocity = 99999.0
        } else {
            act.velocity = -99999.0
        }
        act.jump = true

        return act
    }

}
