package core

import Debug
import model.Game
import model.Point2D
import model.Unit
import model.UnitAction
import strats.AbstractStrategy
import util.then

class GoToTileStrat(tileTarget: Point2D, val label: String, val forceJumps: Boolean = false) : AbstractStrategy() {
    val target = tileTarget.copy().plus(0.5, 0.0)

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        val action = super.getAction(me, game, debug)

        if (target.x > me.position.x) {
            action.velocity = 9999.0;
        } else {
            action.velocity = -9999.0;
        }
        forceJumps.then {
            action.jump = true
        }
        return action
    }

    override fun toString(): String {
        return "GoToTileStrat(label='$label', target=$target)"
    }

}
