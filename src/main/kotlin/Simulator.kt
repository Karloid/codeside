@file:Suppress("NOTHING_TO_INLINE")

import model.Game
import model.Point2D
import model.Tile
import model.Unit
import kotlin.math.abs


var emptySimCount: Long = 0
var fullSimTickCount: Long = 0

class Simulator(val game: Game, val mStrt: MyStrategy) {

    @JvmField
    var ticksCacled: Int = 0

    @JvmField
    var robotBallTouches = IntArray(8)

    var resultKill: Boolean? = null

    @JvmField
    var microTicks: Int = 10

    var customRenderArray: MutableList<Any>? = null

    val metainfo: SimMetaInfo by lazy { SimMetaInfo() }

    val prop = game.properties


    fun move(e: Unit, delta_time: Float) {
        /* e.velocity = e.velocity.clamp(copyRules.MAX_ENTITY_SPEED)
         e.position += e.velocity * delta_time
         e.position.y -= copyRules.GRAVITY * delta_time * delta_time / 2
         e.velocity.y -= copyRules.GRAVITY * delta_time*/
    }


    fun tick() {
        ticksCacled++


        var delta_time = 1f / game.properties.ticksPerSecond
        for (i in 0 until microTicks) {
            update(delta_time / microTicks)
        }


        for (unit in game.units) {
            val positions = metainfo.movements.getOrPut(unit.id) { mutableListOf() }
            positions.add(unit.position.copy())
        }
    }

    private fun update(delta_time: Double) {
        for (unit in game.units) {
            val action = unit.simAction

            //horizontal movement
            val maxSpeed = game.properties.unitMaxHorizontalSpeed
            var velocity = action.velocity
            velocity = maxOf(-maxSpeed, velocity)
            velocity = minOf(maxSpeed, velocity)
            velocity *= delta_time
            val newPosition = unit.position.copy().plus(velocity, 0.0)

            //check horizontal collide with walls
            if (!isHorizontalCollideLevel(newPosition, unit)) {
                if (noCollideWithOtherUnits(unit, newPosition))
                    unit.position = newPosition
            }

            // vertical movement
        }
    }

    private fun noCollideWithOtherUnits(myUnit: Unit, newPosition: Point2D) =
        game.units.none { it != myUnit && abs(it.position.y - newPosition.y) < myUnit.size.y && abs(it.position.x - newPosition.x) < myUnit.size.x }

    private fun isHorizontalCollideLevel(newPosition: Point2D, unit: Unit): Boolean {

        val yBot = newPosition.y
        val yTop = newPosition.y + unit.size.y

        val unitHalfXSize = unit.size.x / 2f
        for (yToCheck in yBot.toInt()..yTop.toInt()) {
            val xLeft = (newPosition.x - unitHalfXSize).toInt()
            val xRight = (newPosition.x + unitHalfXSize).toInt()
            for (xToCheck in xLeft..xRight) {
                val tile = game.level.tiles.getFast(xToCheck, yToCheck)
                if (tile == Tile.WALL) {
                    return true
                }
            }
        }
        return false
    }

    class Dan(@JvmField var distance: Double, @JvmField var normal: Point2D) {
        operator fun component1() = distance
        operator fun component2() = normal
    }

}
