package strats

import Debug
import util.fori
import model.*
import model.Unit
import kotlin.math.max

open class AbstractStrategy : Strategy {
    val prevActions = mutableListOf<UnitAction>()
    lateinit var debug: Debug
    lateinit var me: Unit
    lateinit var game: Game

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        this.me = me
        this.game = game
        this.debug = debug

        return UnitAction()
    }

    fun signedDist(pointToCheck: Point2D, target: Unit?): Double {
        var minDist = Double.MAX_VALUE

        val wallSize = Point2D(0.5, 0.5)
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
        }.minBy { it.position.distance(me.position) }
    }

    protected fun getClosestEnemy(): Unit? {
        return game.units.filter { it.isMy().not() }.minBy { it.position.distance(me.position) }
    }

    private fun Unit.isMy(): Boolean {
        return me.playerId == playerId
    }

    protected fun Point2D.distMe(): Double {
        return this.distance(me.position)
    }
}

fun isRocketAffected(target: Unit, pointToCheck: Point2D, explosionRadius: Double): Boolean {
    val center = target.center()
    return (center - pointToCheck).abs().let {
        it.x <= explosionRadius &&
                it.y <= explosionRadius
    }
}