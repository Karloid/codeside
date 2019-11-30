import Direction.LEFT
import Direction.RIGHT
import MainKt.Companion.myLog
import model.*
import model.Unit
import kotlin.reflect.KClass

class MyStrategy : Strategy {

    private lateinit var debug: Debug
    private lateinit var me: Unit
    private lateinit var game: Game

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        this.me = me
        this.game = game
        this.debug = debug
        val action = smartGuy(debug, game, me)
        printAction(action)
        return action
    }

    private inline fun printAction(action: UnitAction) {
        myPrint { "action:$action" }
    }

    private inline fun myPrint(function: () -> String) {
         myLog { game.currentTick.toString() + ": " + function() }
    }

    private fun smartGuy(debug: Debug, game: Game, me: Unit): UnitAction {
        val action = UnitAction()

        debug.draw(CustomData.Line(game.units[0].position, game.units[1].position, 1 / 20f, ColorFloat(1f, 0f, 0f, 1f)))

        val nearestEnemy: Unit? = getClosestEnemy()
        val nearestWeapon = getClosest(Item.Weapon::class)
        val nearestHealth = getClosest(Item.HealthPack::class)

        var targetPos: Point2D = me.position

        if (me.weapon == null && nearestWeapon != null) {
            targetPos = nearestWeapon.position
        } else if (nearestHealth != null) {
            targetPos = nearestHealth.position
        } else if (me.weapon?.typ == WeaponType.PISTOL && nearestWeapon != null) {
            targetPos = nearestWeapon.position
            action.swapWeapon = isClose(targetPos)
        } else if (nearestEnemy != null) {
            targetPos = nearestEnemy.position
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))

        var aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = nearestEnemy.position.copy() - me.position
        }
        var jump = targetPos.y > me.position.y;
        if (targetPos.x > me.position.x && game.getTile(me.position, RIGHT) == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < me.position.x && game.getTile(me.position, LEFT) == Tile.WALL) {
            jump = true
        }

        action.velocity = (targetPos.x - me.position.x) * 10000
        action.jump = jump
        action.jumpDown = !jump
        action.aim = aim
        action.shoot = true
        action.plantMine = false

        return action
    }

    private fun isClose(targetPos: Point2D) = targetPos.distMe() < 1

    private fun <T : Any> getClosest(type: KClass<T>): LootBox? {
        return game.lootBoxes.filter { it.item::class == type }.minBy { it.position.distance(me.position) }
    }

    private fun getClosestEnemy(): Unit? {
        return game.units.filter { it.isMy().not() }.minBy { it.position.distance(me.position) }
    }

    companion object {
        internal fun distanceSqr(a: Point2D, b: Point2D): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }

    private fun Unit.isMy(): Boolean {
        return me.playerId == playerId
    }

    private fun Point2D.distMe(): Double {
        return this.distance(me.position)
    }
}


