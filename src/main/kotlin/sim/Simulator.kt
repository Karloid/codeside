@file:Suppress("NOTHING_TO_INLINE")

package sim

import core.MyStrategy
import ifEnabledLog
import model.*
import model.Unit
import util.then
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


        ifEnabledLog {
            for (unit in game.units) {
                val positions = metainfo.movements.getOrPut(unit.id) { mutableListOf() }
                positions.add(unit.position.copy())
            }

            for (bullet in game.bullets) {
                metainfo.bulletsHistory.add(bullet.copy())
            }
        }
    }

    private fun update(delta_time: Double) {

        //move units
        for (unit in game.units) {
            val action = unit.simAction

            //horizontal movement
            val maxSpeed = game.properties.unitMaxHorizontalSpeed
            var velocity = action.velocity
            velocity = maxOf(-maxSpeed, velocity)
            velocity = minOf(maxSpeed, velocity)
            velocity *= delta_time
            val newPosHor = unit.position.copy().plus(velocity, 0.0)

            //check horizontal collide with walls
            if (!isHorizontalCollideLevel(newPosHor, unit)) {
                if (noCollideWithOtherUnitsHorizontally(unit, newPosHor))
                    unit.position = newPosHor
            }

            // vertical movement UP
            if (action.jump && unit.jumpState.canJump) {
                unit.onGround = false
                unit.jumpState.maxTime = unit.jumpState.maxTime - delta_time
                if (unit.jumpState.maxTime <= 0) {
                    unit.jumpState.maxTime = 0.0
                    unit.jumpState.canJump = false;
                }

                if (unit.jumpState.canJump) {
                    if (isCollideVerticallyTop(delta_time, unit)) {

                        unit.jumpState.canJump = false
                        unit.jumpState.maxTime = 0.0
                        //we are failing after stuck
                    }
                }
            }
            updateUnitLadder(unit)

            //check we still on ground
            if (!unit.simAction.jump && unit.onGround && !unit.onLadder) {
                if (!isVerticalCollideLevel(unit.position, unit, false, false)) {
                    if (noCollideWithOtherUnitsVertically(unit, unit.position, false)) {
                        unit.onGround = false
                    }
                }
            }

            // hmm something
            if (!action.jump && unit.jumpState.canJump) {
                unit.jumpState.canJump = false
                unit.jumpState.maxTime = 0.0
            }

            //failing bottom
            val isFailing = !unit.jumpState.canJump && !unit.onGround && !unit.onLadder
            val isOnLadderDown = unit.simAction.jumpDown && unit.onLadder
            val isJumpDownFromPlatform = unit.simAction.jumpDown && unit.onGround
            if (isFailing || isOnLadderDown || isJumpDownFromPlatform) {
                if (isCollideVerticallyBot(delta_time, unit, action.jumpDown)) {
                    unit.onGround = true
                    unit.jumpState.canJump = true
                    unit.jumpState.maxTime = game.properties.unitJumpTime
                    //we are failing after stuck
                }
            }

            //check ladder
            updateUnitLadder(unit)
            //TODO jump pad


            //TODO fighting
            val x = 10
        }


        var bulletsToRemove = ArrayList<Bullet>(0)
        game.bullets.forEach { bullet ->
            bullet.position.plus(bullet.velocity.x * delta_time, bullet.velocity.y * delta_time)

            //check unit collide
            for (unit in game.units) {
                isCollide(bullet, unit).then {
                    unit.health -= bullet.damage

                    onBulletCollide(bullet, unit)

                    bulletsToRemove.add(bullet)
                    metainfo.unitHitRegs.add(bullet.position.copy())
                    return@forEach
                }
            }

            if (!bulletsToRemove.contains(bullet)) {
                isCollideWalls(bullet).then {
                    onBulletCollide(bullet, null)
                    bulletsToRemove.add(bullet)
                }
            }
        }

        bulletsToRemove.isNotEmpty().then {
            game.bullets = game.bullets.filter { !bulletsToRemove.contains(it) }.toTypedArray()
        }
    }

    private fun onBulletCollide(bullet: Bullet, unitThatCollide: Unit?) {
        bullet.explosionParams?.damage?.let { damage ->
            val affectedUnits = mutableListOf<Point2D>()
            for (unit in game.units) {
                val distToBullet = (unit.position.copy().plus(0.0, unit.size.y / 2) - bullet.position).abs()
                val radius = bullet.explosionParams!!.radius * 1.2f
                if (distToBullet.x <= radius && distToBullet.y <= radius) {
                    unit.health -= damage
                    affectedUnits.add(unit.position.copy().plus(0.0, unit.size.y / 2))
                }
            }

            val explosion = Explosion(bullet.position.copy(), affectedUnits, bullet.explosionParams!!.radius)
            metainfo.explosions.add(explosion)
        }
    }

    private fun isCollideWalls(bullet: Bullet): Boolean {
        val tiles = game.level.tiles
        val halfSize = bullet.size / 2

        var x = (bullet.position.x - halfSize).toInt()
        var y = (bullet.position.y - halfSize).toInt()
        tiles.getFast(x, y).equals(Tile.WALL).then {
            return true
        }

        x = (bullet.position.x - halfSize).toInt()
        y = (bullet.position.y + halfSize).toInt()
        tiles.getFast(x, y).equals(Tile.WALL).then {
            return true
        }

        x = (bullet.position.x + halfSize).toInt()
        y = (bullet.position.y + halfSize).toInt()
        tiles.getFast(x, y).equals(Tile.WALL).then {
            return true
        }

        x = (bullet.position.x + halfSize).toInt()
        y = (bullet.position.y - halfSize).toInt()
        tiles.getFast(x, y).equals(Tile.WALL).then {
            return true
        }

        return false
    }

    private fun isCollide(bullet: Bullet, unit: Unit): Boolean {
        if (unit.id == bullet.unitId) {
            return false
        }
        return abs(bullet.position.x - unit.position.x) < bullet.size / 2 + unit.size.x / 2 &&
                abs(bullet.position.y - (unit.position.y + unit.size.y / 2)) < bullet.size / 2 + unit.size.y / 2
    }

    private fun updateUnitLadder(unit: Unit) {
        //check ladder
        unit.onLadder = isUnitOnLadder(unit.position, unit.size)
        if (unit.onLadder) {
            unit.onGround = false
            unit.jumpState.canJump = true
            unit.jumpState.maxTime = game.properties.unitJumpTime
        }
    }

    private fun isUnitOnLadder(position: Point2D, unitSize: Point2D): Boolean {
        val x = position.intX
        val botY = position.intY

        val topY = (position.y + unitSize.y / 2.0).toInt()

        return game.level.tiles.getFast(x, botY) == Tile.LADDER || game.level.tiles.getFast(x, topY) == Tile.LADDER
    }

    private fun isCollideVerticallyTop(delta_time: Double, unit: Unit): Boolean {
        val jumpSpeed = game.properties.unitJumpSpeed * delta_time
        val newPos = unit.position.copy().plus(0.0, jumpSpeed)

        if (!isVerticalCollideLevel(newPos, unit, true, false)) {
            if (noCollideWithOtherUnitsVertically(unit, newPos, true)) {
                unit.position = newPos
                return false
            }
        }
        return true
    }

    private fun isCollideVerticallyBot(delta_time: Double, unit: Unit, jumpDown: Boolean): Boolean {
        val jumpSpeed = game.properties.unitFallSpeed * delta_time
        val newPos = unit.position.copy().minus(0.0, jumpSpeed)

        if (!isVerticalCollideLevel(newPos, unit, false, jumpDown)) {
            if (noCollideWithOtherUnitsVertically(unit, newPos, false)) {
                unit.position = newPos
                return false
            }
        }
        return true
    }

    private fun noCollideWithOtherUnitsVertically(myUnit: Unit, newPosition: Point2D, checkTop: Boolean): Boolean {
        return game.units.none {
            it != myUnit &&
                    (checkTop.then { it.position.y > newPosition.y } ?: run { it.position.y < newPosition.y }) &&
                    abs(it.position.y - newPosition.y) < myUnit.size.y &&
                    abs(it.position.x - newPosition.x) < myUnit.size.x
        }
    }

    private fun isVerticalCollideLevel(
        newPosition: Point2D,
        unit: Unit,
        isTopCheck: Boolean,
        jumpDown: Boolean
    ): Boolean {
        val yBot = newPosition.y.toInt()
        val yTop = (newPosition.y + unit.size.y).toInt()


        val unitHalfXSize = unit.size.x / 2f

        val yCheck = isTopCheck.then { yTop } ?: yBot

        val respectNotOnlyWalls = !jumpDown && !isTopCheck
        return checkVerticalWalls(newPosition, unitHalfXSize, yCheck, respectNotOnlyWalls, unit)
    }

    private fun checkVerticalWalls(
        newPosition: Point2D,
        unitHalfXSize: Double,
        yToCheck: Int,
        respectNotOnlyWalls: Boolean,
        unit: Unit
    ): Boolean {
        val xLeft = (newPosition.x - unitHalfXSize).toInt()
        val xRight = (newPosition.x + unitHalfXSize).toInt()

        if (respectNotOnlyWalls) {
            if (isUnitOnLadder(newPosition, game.properties.unitSize)) {
                return true
            }
        }

        for (xToCheck in xLeft..xRight) {
            val tile = game.level.tiles.getFast(xToCheck, yToCheck)
            if (tile == Tile.WALL) {
                return true
            }

            if (respectNotOnlyWalls && (tile == Tile.PLATFORM) && yToCheck != unit.position.intY) {
                return true
            }
        }
        return false
    }

    private fun noCollideWithOtherUnitsHorizontally(myUnit: Unit, newPosition: Point2D) =
        game.units.none { it != myUnit && abs(it.position.y - newPosition.y) < myUnit.size.y && abs(it.position.x - newPosition.x) < myUnit.size.x }

    private fun isHorizontalCollideLevel(newPosition: Point2D, unit: Unit): Boolean {

        val yBot = newPosition.y
        val yTop = newPosition.y + unit.size.y

        val unitHalfXSize = unit.size.x / 2f
        for (yToCheck in yBot.toInt()..yTop.toInt()) {
            val xLeft = (newPosition.x - unitHalfXSize).toInt()
            val xRight = (newPosition.x + unitHalfXSize).toInt()
            for (xToCheck in xLeft..xRight) {
                if (xToCheck == xLeft || xToCheck == xRight) {
                    val tile = game.level.tiles.getFast(xToCheck, yToCheck)
                    if (tile == Tile.WALL) {
                        return true
                    }
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
