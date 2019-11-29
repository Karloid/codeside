import model.*
import model.Point2D
import model.Unit

class MyStrategy : Strategy {

    private lateinit var debug: Debug
    private lateinit var me: Unit
    private lateinit var game: Game

    override fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        this.me = me
        this.game = game
        this.debug = debug
        val action = smartGuy(debug, game, me)
        return action
    }

    private fun smartGuy(debug: Debug, game: Game, me: Unit): UnitAction {
        debug.draw(CustomData.Line(game.units[0].position, game.units[1].position, 1 / 20f, ColorFloat(1f, 0f, 0f, 1f)))

        var nearestEnemy: Unit? = null
        for (other in game.units) {
            if (other.playerId != me.playerId) {
                if (nearestEnemy == null || distanceSqr(
                        me.position,
                        other.position
                    ) < distanceSqr(me.position, nearestEnemy.position)
                ) {
                    nearestEnemy = other
                }
            }
        }
        var nearestWeapon: LootBox? = null
        for (lootBox in game.lootBoxes) {
            if (lootBox.item is Item.Weapon) {
                if (nearestWeapon == null || distanceSqr(
                        me.position,
                        lootBox.position
                    ) < distanceSqr(me.position, nearestWeapon.position)
                ) {
                    nearestWeapon = lootBox
                }
            }
        }
        var targetPos: Point2D = me.position
        if (me.weapon == null && nearestWeapon != null) {
            targetPos = nearestWeapon.position
        } else if (nearestEnemy != null) {
            targetPos = nearestEnemy.position
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))

        var aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Point2D(
                nearestEnemy.position.x - me.position.x,
                nearestEnemy.position.y - me.position.y
            )
        }
        var jump = targetPos.y > me.position.y;
        if (targetPos.x > me.position.x && game.level.tiles[(me.position.x + 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < me.position.x && game.level.tiles[(me.position.x - 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        val action = UnitAction()
        action.velocity = targetPos.x - me.position.x
        action.jump = jump
        action.jumpDown = !jump
        action.aim = aim
        action.shoot = true
        action.swapWeapon = false
        action.plantMine = false
        return action
    }

    companion object {
        internal fun distanceSqr(a: Point2D, b: Point2D): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}
