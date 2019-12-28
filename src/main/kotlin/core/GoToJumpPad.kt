package core

import Debug
import model.Game
import model.Point2D
import model.Unit
import model.UnitAction
import strats.AbstractStrategy

class GoToJumpPad(tileTarget: Point2D) : AbstractStrategy() {
    val target = tileTarget.copy().plus(0.5, 0.0)

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        val action = super.getAction(me, game, debug)

        if (target.x > me.position.x) {
            action.velocity = 9999.0;
        } else {
            action.velocity = -9999.0;
        }
        return action
    }

    override fun toString(): String {
        return "GoToJumpPad(target=$target)"
    }

}
