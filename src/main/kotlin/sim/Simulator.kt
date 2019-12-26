@file:Suppress("NOTHING_TO_INLINE")

package sim

import ifEnabledLog
import model.*
import model.Unit
import util.PlainArray
import util.Ref
import util.fori
import util.then
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min


class Simulator(val game: Game) {

    @JvmField
    var ticksCacled: Int = 0

    @JvmField
    var resultKill: Boolean? = null

    @JvmField
    var microTicks: Int = 10

    val metainfo: SimMetaInfo by lazy { SimMetaInfo() }

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

            if (isCollideWithTramp(unit)) {
                unit.jumpState.canCancel = false
                unit.jumpState.canJump = true // ?
                unit.jumpState.maxTime = game.properties.jumpPadJumpTime
                unit.jumpState.speed = game.properties.jumpPadJumpSpeed
            }

            //hack
            val itIsJumpPadJump = unit.isJumpPadJump()

            // vertical movement UP jump
            if ((action.jump && unit.jumpState.canJump) || itIsJumpPadJump) {
                if (!itIsJumpPadJump) {
                    unit.jumpState.canCancel = true
                    unit.jumpState.speed = game.properties.unitJumpSpeed
                }
                unit.onGround = false
                unit.jumpState.maxTime = unit.jumpState.maxTime - delta_time
                if (unit.jumpState.maxTime <= 0) {
                    unit.jumpState.speed = 0.0
                    unit.jumpState.maxTime = 0.0
                    unit.jumpState.canJump = false;
                    unit.jumpState.canCancel = false;
                }

                if (unit.jumpState.canJump || itIsJumpPadJump) {
                    if (isCollideVerticallyTop(delta_time, unit)) {

                        unit.jumpState.canJump = false
                        unit.jumpState.maxTime = 0.0
                        unit.jumpState.speed = 0.0
                        //we are failing after stuck
                    } else {
                        //TODO
                    }
                }
            }
            updateUnitLadder(unit)

            //check we still on ground
            if (!action.jump && !itIsJumpPadJump && !unit.onLadder && !(!unit.onGround && !unit.jumpState.canJump)) {
                if (!isVerticalCollideLevel(
                        unit.position,
                        unit,
                        false,
                        false
                    ) || !isVerticalCollideLevel(unit.position.copy().minus(0.0, 0.1), unit, false, false)
                ) {
                    if (noCollideWithOtherUnitsVertically(unit, unit.position, false)) {
                        unit.onGround = false
                        unit.jumpState.canJump = false
                    }
                }
            }

            // hmm something  // canceling jump?
            if (!action.jump && !itIsJumpPadJump && unit.jumpState.canCancel) {
                unit.jumpState.canJump = false
                unit.jumpState.maxTime = 0.0
                unit.jumpState.speed = 0.0
            }

            //failing bottom
            val isFailing = !unit.jumpState.canJump && !unit.onGround && !unit.onLadder
            val isOnLadderDown = unit.simAction.jumpDown && unit.onLadder
            val isJumpDownFromPlatform = unit.simAction.jumpDown && unit.onGround
            if (isFailing || isOnLadderDown || isJumpDownFromPlatform) {
                if (!isCollideWithTramp(unit) && isCollideVerticallyBot(delta_time, unit, action.jumpDown)) {
                    unit.onGround = true
                    unit.jumpState.canJump = true
                    unit.jumpState.maxTime = game.properties.unitJumpTime
                    unit.jumpState.canCancel = false
                    //we are failing after stuck
                }
            }

            //check ladder
            updateUnitLadder(unit)

            //TODO fighting
            val x = 10
        }

        //BULLETS

        var bulletsToRemove = ArrayList<Bullet>(0)
        game.bullets.forEach { bullet ->
            bullet.position.plus(bullet.velocity.x * delta_time, bullet.velocity.y * delta_time)

            //check unit collide
            for (unit in game.units) {
                isCollide(bullet, unit).then {
                    val damage = bullet.damage
                    onUnitTakeDamage(unit, damage)

                    onBulletCollide(bullet)

                    bulletsToRemove.add(bullet)
                    metainfo.unitHitRegs.add(BulletHitPoint(bullet.position.copy(), bullet))
                    return@forEach
                }
            }

            if (!bulletsToRemove.contains(bullet)) {
                isCollideWalls(bullet, game.level.tiles).then {
                    onBulletCollide(bullet)
                    bulletsToRemove.add(bullet)

                    if (bullet.explosionParams != null) {
                        metainfo.unitHitRegs.add(BulletHitPoint(bullet.position.copy(), bullet))
                    }
                }
            }
        }

        bulletsToRemove.isNotEmpty().then {
            game.bullets = game.bullets.filter { !bulletsToRemove.contains(it) }.toTypedArray()
        }

        var deadUnits: MutableList<Unit>? = null
        game.units.forEach {
            it.health = maxOf(it.health, 0)
            if (it.health == 0) {
                deadUnits = deadUnits ?: mutableListOf()
                deadUnits!!.add(it)
            }
        }

        for (mine in game.mines) {
            when (mine.state) {
                MineState.IDLE -> {
                    if (isTriggered(mine)) {
                        mine.state = MineState.TRIGGERED
                        mine.timer = game.properties.mineTriggerTime
                    }
                }
                MineState.PREPARING -> {
                    mine.timer = mine.timer ?: 0.0 - delta_time
                    if (mine.timer!! < 0) {
                        mine.state = MineState.IDLE
                    }
                }
                MineState.TRIGGERED -> {
                    mine.timer = mine.timer ?: 0.0 - delta_time
                    if (mine.timer!! < 0) {
                        mine.state = MineState.EXPLODED
                    }
                }
                MineState.EXPLODED -> {
                    mine.explosionParams.damage.let { damage ->
                        val affectedUnits = mutableListOf<Point2D>()
                        for (unit in game.units) {
                            val isAffected = unitAffectedByExplosion(unit, mine.explosionParams!!.radius, mine.center())
                            if (isAffected) {
                                onUnitTakeDamage(unit, damage)
                                affectedUnits.add(unit.position.copy())
                            }
                        }

                        ifEnabledLog {
                            val explosion = Explosion(bullet.position.copy(), affectedUnits, bullet.explosionParams!!.radius)
                            metainfo.explosions.add(explosion)
                        }
                    }
                }
            }
        }

        if (deadUnits != null) {
            ifEnabledLog {
                deadUnits!!.fori {
                    metainfo.deadUnits.add(it.position.copy())
                }

            }
            game.units = game.units.filter { !deadUnits!!.contains(it) }.toTypedArray()
        }

        //health

        game.units.forEach { unit ->
            if (unit.health != game.properties.unitMaxHealth) {
                val health = getCollidedHealth(unit)
                if (health != null) {
                    unit.health += game.properties.healthPackHealth
                    unit.health = min(unit.health, game.properties.unitMaxHealth)
                    game.lootBoxes = game.lootBoxes.filter { it != health }.toTypedArray() //picked health
                }
            }
        }

    }

    private fun isTriggered(mine: Mine): Boolean {
        val mineX = mine.position.x
        val mineY = mine.position.y + mine.size.y / 2
        return game.units.any { unit ->
            val unitCenter = unit.center()
            val deltaX = (unitCenter.x - mineX).absoluteValue
            val deltaY = (unitCenter.y - mineY).absoluteValue
            deltaX <= unit.size.x /2 + game.properties.mineTriggerRadius &&
            deltaY <= unit.size.y /2 + game.properties.mineTriggerRadius

        }
    }

    private fun getCollidedHealth(unit: Unit): LootBox? {
        return game.lootBoxes.firstOrNull { lootbox ->
            val item = lootbox.item
            (item is Item.HealthPack).not().then { return@firstOrNull false }
            val deltaX = (unit.position.x - lootbox.position.x).absoluteValue
            val deltaY = ((unit.position.y + unit.size.y / 2) - (lootbox.position.y + lootbox.size.y / 2)).absoluteValue
            if (deltaX < unit.size.x / 2 + lootbox.size.x / 2 &&
                deltaY < unit.size.y / 2 + lootbox.size.y / 2
            ) {
                return@firstOrNull true
            }

            return@firstOrNull false
        }
    }

    private fun getCollidedMine(unit: Unit): LootBox? {
        return game.lootBoxes.firstOrNull { lootbox ->
            val item = lootbox.item
            (item is Item.HealthPack).not().then { return@firstOrNull false }
            val deltaX = (unit.position.x - lootbox.position.x).absoluteValue
            val deltaY = ((unit.position.y + unit.size.y / 2) - (lootbox.position.y + lootbox.size.y / 2)).absoluteValue
            if (deltaX < unit.size.x / 2 + lootbox.size.x / 2 &&
                deltaY < unit.size.y / 2 + lootbox.size.y / 2
            ) {
                return@firstOrNull true
            }

            return@firstOrNull false
        }
    }


    private fun isCollideWithTramp(unit: Unit): Boolean {
        val unitHalfXSize = unit.size.x / 2
        val newPosition = unit.position

        val yToCheck = unit.position.y.toInt()
        val yToCheck2 = (unit.position.y + unit.size.y).toInt()

        val xLeft = (newPosition.x - unitHalfXSize).toInt()
        val xRight = (newPosition.x + unitHalfXSize).toInt()
        for (xToCheck in xLeft..xRight) {
            if (xToCheck == xLeft || xToCheck == xRight) {
                val tile = game.level.tiles.getFast(xToCheck, yToCheck)
                if (tile === Tile.JUMP_PAD) {
                    return true
                }
                val tile2 = game.level.tiles.getFast(xToCheck, yToCheck2)
                if (tile2 === Tile.JUMP_PAD) {
                    return true
                }
            }
        }
        return false
    }

    private fun onBulletCollide(bullet: Bullet) {
        bullet.explosionParams?.damage?.let { damage ->
            val affectedUnits = mutableListOf<Point2D>()
            for (unit in game.units) {
                val isAffected = unitAffectedByExplosion(unit, bullet.explosionParams!!.radius, bullet.position)
                if (isAffected) {
                    onUnitTakeDamage(unit, damage)
                    affectedUnits.add(unit.position.copy())
                }
            }

            ifEnabledLog {
                val explosion = Explosion(bullet.position.copy(), affectedUnits, bullet.explosionParams!!.radius)
                metainfo.explosions.add(explosion)
            }
        }
    }

    private fun onUnitTakeDamage(unit: Unit, damage: Int) {
        unit.health -= damage
        metainfo.unitDamage.getOrPut(unit.id, { Ref(0.0) }).ref += damage
    }

    private fun isCollide(bullet: Bullet, unit: Unit): Boolean {
        if (unit.id == bullet.unitId) {
            return false
        }
        val bulletSize = bullet.size
        val size = bulletSize * 1.05
        val bulletPos = bullet.position
        return isCollide(bulletPos, unit, size)
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
        val jumpSpeed = unit.jumpState.speed * delta_time
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

    fun noCollideWithOtherUnitsVertically(myUnit: Unit, newPosition: Point2D, checkTop: Boolean): Boolean {
        val unitSize = myUnit.size
        return game.units.none {
            val pos = it.position
            it != myUnit &&
                    (checkTop.then { pos.y > newPosition.y } ?: run { pos.y < newPosition.y }) &&
                    abs(pos.y - newPosition.y) < unitSize.y &&
                    abs(pos.x - newPosition.x) < unitSize.x
        }
    }

    fun isVerticalCollideLevel(
        newPosition: Point2D,
        unit: Unit,
        isTopCheck: Boolean,
        jumpDown: Boolean
    ): Boolean {
        val yBot = newPosition.y.toInt()
        val yTop = (newPosition.y + unit.size.y).toInt()

        val unitHalfXSize = unit.size.x / 2f

        val yCheck: Int = if (isTopCheck) yTop else yBot

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

    companion object {
        fun unitAffectedByExplosion(unit: Unit, explosRadius: Double, bulletCenter: Point2D): Boolean {
            val distToBullet = (unit.position.copy().plus(0.0, unit.size.y / 2) - bulletCenter).abs()
            val radius = explosRadius * 1.2f
            val isAffected = distToBullet.x - unit.size.x / 2 <= radius && distToBullet.y - unit.size.y / 2 <= radius
            return isAffected
        }

        fun isCollide(bulletPos: Point2D, unit: Unit, size: Double) =
            abs(bulletPos.x - unit.position.x) < size / 2 + unit.size.x / 2 &&
                    abs(bulletPos.y - (unit.position.y + unit.size.y / 2)) < size / 2 + unit.size.y / 2

        fun isCollideWalls(bullet: Bullet, tiles: PlainArray<Tile>): Boolean {
            val size = bullet.size
            val bulletPos = bullet.position

            return isCollideWalls(bulletPos, size, tiles)
        }

        fun isCollideWalls(
            bulletPos: Point2D,
            size: Double,
            tiles: PlainArray<Tile>
        ): Boolean {
            val halfSize = size / 2
            var x = (bulletPos.x - halfSize).toInt()
            var y = (bulletPos.y - halfSize).toInt()
            (tiles.getFast(x, y) === Tile.WALL).then {
                return true
            }

            x = (bulletPos.x - halfSize).toInt()
            y = (bulletPos.y + halfSize).toInt()
            (tiles.getFast(x, y) === Tile.WALL).then {
                return true
            }

            x = (bulletPos.x + halfSize).toInt()
            y = (bulletPos.y + halfSize).toInt()
            (tiles.getFast(x, y) === Tile.WALL).then {
                return true
            }

            x = (bulletPos.x + halfSize).toInt()
            y = (bulletPos.y - halfSize).toInt()
            (tiles.getFast(x, y) === Tile.WALL).then {
                return true
            }

            return false
        }
    }

    private fun Unit.isJumpPadJump(): Boolean {
        return jumpState.speed == game.properties.jumpPadJumpSpeed && jumpState.maxTime > 0.0
    }
}

