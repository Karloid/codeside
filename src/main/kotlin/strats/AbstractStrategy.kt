package strats

import Debug
import MainKt
import core.pathDist
import ifEnabledLog
import model.*
import model.Unit
import sim.Simulator
import util.fori
import kotlin.math.max

val wallSize = Point2D(0.5, 0.5)

open class AbstractStrategy : StrategyAdvCombined {
    val prevActions = mutableMapOf<Int, MutableList<UnitAction>>()
    lateinit var debug: Debug
    lateinit var me: Unit
    lateinit var game: Game

    override var isReal: Boolean = false

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {

        this.me = me
        this.game = game
        this.debug = debug

        return UnitAction()
    }

    fun getMaxJumpTicks(): Double {
        return game.properties.unitJumpTime * game.properties.ticksPerSecond
    }

    fun signedDist(pointToCheck: Point2D, target: Unit?): Double {
        var minDist = Double.MAX_VALUE

        //TODO optimizations based on square dist
        if (target != null) {
            minDist = signedDstToBox(pointToCheck.copy(), target.center(), target.size.copy().mul(0.5))
        } else {
            game.level.walls.fori {
                val rectCenter = it.copy().plus(0.5, 0.5)
                minDist = minOf(signedDstToBox(pointToCheck.copy(), rectCenter, wallSize), minDist)
            }
        }
        return minDist
    }

    private fun signedDstToBox(pointToCheck: Point2D, rectCenter: Point2D, rectSize: Point2D): Double {
        val offset = (pointToCheck - rectCenter).abs() - rectSize
        val unsignedDst = max(offset, 0.0).length()
        val dstInsideBox = min(offset, 0.0).max()
        return unsignedDst + dstInsideBox
    }

    private fun max(point: Point2D, value: Double): Point2D {
        return Point2D(max(point.x, value), max(point.y, value))
    }

    private fun min(point: Point2D, value: Double): Point2D {
        return Point2D(Math.min(point.x, value), Math.min(point.y, value))
    }

    protected fun getClosestWeaponItem(weaponType: WeaponType): LootBox? {
        return game.lootBoxes.filter {
            val item = it.item
            item is Item.Weapon && item.weaponType == weaponType
        }.minBy { it.position.pathDist(me.position) }
    }

    protected fun getClosestEnemy(): Unit? {
        val anotherMe = getAnotherUnit()
        val enemies = game.units.filter { it.isMy().not() }
        val closest = enemies.minBy {
            var dist = it.position.pathDist(me.position)
           // if (anotherMe != null) {
           //     dist += it.position.distance(anotherMe.position)
           // }
            dist
        } ?: return null
        
        enemies.firstOrNull { !it.isMy() && closest != it }?.let { lowHealth ->
            if (lowHealth.health < closest.health && lowHealth.position.pathDist(closest.position) < 4) {
                return lowHealth
            }

        }
        return closest
    }

    fun Unit.isMy(): Boolean {
        return me.playerId == playerId
    }

    protected fun Point2D.distMe(): Double {
        return this.distance(me.position)
    }

    inline fun log(function: () -> String) {
        if (!isReal) {
            return
        }
        MainKt.log { game.currentTick.toString() + ": " + me.id + ">" + function() }
    }

    inline fun d(function: () -> kotlin.Unit) {
        if (isReal) {
            ifEnabledLog(function)
        }
    }

    fun getAnotherUnit(): Unit? {
        return game.units.firstOrNull { it.isMy() && me.id != it.id }
    }

    fun fastJumpFix(unit: Unit, unitAction: UnitAction) {
        val unitYDecimal = unit.position.y % 1
        if (unitAction.jump && unit.jumpState.maxTime < 0.30150 && unitYDecimal < 0.20) {
            if (Simulator(game).isVerticalCollideLevel(
                    unit.position.copy().minus(0.0, unitYDecimal + 0.1),
                    unit, false, false
                )
            ) {
                //fast jump
                unitAction.jump = false
            }
        }
    }
}