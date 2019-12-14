package strats

import Debug
import model.Game
import model.Unit
import model.UnitAction

class MoveStrategy(val leftRight: MoveLeftRight, val moveUpDown: MoveUpDown) : StrategyAdvCombined {
    override var isReal: Boolean = false

    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        val unitAction = UnitAction()

        when (leftRight) {
            MoveLeftRight.LEFT -> unitAction.velocity = -9999.0
            MoveLeftRight.STILL -> unitAction.velocity = 0.0
            MoveLeftRight.RIGHT -> unitAction.velocity = 9999.0
        }

        when (moveUpDown) {
            MoveUpDown.UP -> unitAction.jump = true
            MoveUpDown.STILL -> {
            }
            MoveUpDown.DOWN -> unitAction.jumpDown = true
        }

        return unitAction
    }
}

enum class MoveLeftRight() {
    LEFT,
    STILL,
    RIGHT, ;

    companion object {
        val cValues = values()
    }
}

enum class MoveUpDown() {
    UP,
    STILL,
    DOWN, ;

    companion object {
        val cValues = values()
    }
}
