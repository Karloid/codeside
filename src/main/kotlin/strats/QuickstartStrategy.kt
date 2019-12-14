package strats

import Debug
import core.MyStrategy
import model.*

class QuickstartStrategy(myStrategy: MyStrategy) : StrategyAdvCombined {

    override fun getAction(unit: model.Unit, game: Game, debug: Debug): UnitAction {
        var nearestEnemy: model.Unit? = null
        for (other in game.units) {
            if (other.playerId != unit.playerId) {
                if (nearestEnemy == null ||
                    unit.position.sqDistance(other.position) < unit.position.sqDistance(nearestEnemy.position)
                ) {
                    nearestEnemy = other
                }
            }
        }
        var nearestWeapon: LootBox? = null
        for (lootBox in game.lootBoxes) {
            if (lootBox.item is Item.Weapon) {
                if (nearestWeapon == null ||
                    lootBox.position.sqDistance(unit.position) < unit.position.sqDistance(nearestWeapon.position)
                ) {
                    nearestWeapon = lootBox
                }
            }
        }
        var targetPos: Point2D = unit.position
        if (unit.weapon == null && nearestWeapon != null) {
            targetPos = nearestWeapon.position
        } else if (nearestEnemy != null) {
            targetPos = nearestEnemy.position
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))
        var aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Point2D(
                nearestEnemy.position.x - unit.position.x,
                nearestEnemy.position.y - unit.position.y
            )
        }
        var jump = targetPos.y > unit.position.y;
        if (targetPos.x > unit.position.x && game.level.tiles.getFast(
                (unit.position.x + 1).toInt(),
                (unit.position.y).toInt()
            ) == Tile.WALL
        ) {
            jump = true
        }
        if (targetPos.x < unit.position.x && game.level.tiles.getFast(
                (unit.position.x - 1).toInt(),
                (unit.position.y).toInt()
            ) == Tile.WALL
        ) {
            jump = true
        }
        val action = UnitAction()
        action.velocity = targetPos.x - unit.position.x
        action.jump = jump
        action.jumpDown = !jump
        action.aim = aim
        action.shoot = true
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false
        return action
    }
}
