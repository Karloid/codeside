package strats

import Debug
import core.AimScore
import core.MyStrategy
import core.pathDist
import model.*
import model.Unit
import sim.Simulator
import util.Direction
import util.Ref
import util.f
import util.then
import kotlin.math.abs
import kotlin.reflect.KClass

class SmartGuyStrategy(myStrategy: MyStrategy) : AbstractStrategy() {

    private var skippedHealth: Int = 0
    var disableShooting: Boolean = false

    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        super.getAction(unit, game, debug)

        return doSmartGuy()
    }

    private fun doSmartGuy(): UnitAction {
        val action = UnitAction()

        val nearestEnemy: Unit? = getClosestEnemy()
        val nearestWeapon = getClosestItem(Item.Weapon::class)
        var nearestHealth = getHealthPack(null)

        val anotherMe = getAnotherMe()
        if (me.health > game.properties.unitMaxHealth * 0.9) {
            nearestHealth = null
        }

        if (anotherMe != null && (me.health > game.properties.unitMaxHealth * 0.9 ||
                    anotherMe.health < me.health) && nearestHealth != null && skippedHealth < 40
        ) {
            if (me.health < game.properties.unitMaxHealth * 0.9 && anotherMe.health < me.health) {
                nearestHealth = getHealthPack(nearestHealth)

                if (nearestHealth != null) {
                    val anotherX = anotherMe.position.x
                    if ((anotherX > me.position.x && nearestHealth.position.x > me.position.x) ||
                        (anotherX < me.position.x && nearestHealth.position.x < me.position.x)
                    ) {
                        nearestHealth = getHealthPack(null)
                        log { "keep original health" }
                    }
                }
                //   skippedHealth = 0
            } else {
                nearestHealth = null
                //  skippedHealth++
            }
            log { "ignore health" }
        }

        var targetPos: Point2D = me.position

        val wantSwapFromRocketLauncher = wantSwapFromRocketLauncher(me)

        var preferredWeaponToPick: LootBox? = null

        if (wantSwapFromRocketLauncher) {
            getPrefferedWeapon(nearestEnemy)?.let {
                preferredWeaponToPick = it
                action.swapWeapon = isClose(it.position)
            }
        }

        if ((nearestWeapon?.item as? Item.Weapon?)?.weaponType == WeaponType.ROCKET_LAUNCHER
            && wantToPickRocketLauncher() && isClose(nearestWeapon.position)
        ) {
            action.swapWeapon = true
        }

        if (me.weapon == null && nearestWeapon != null) {
            log { "go pick weapon ${nearestWeapon.posInfo()}" }

            targetPos = nearestWeapon.position
        } else if (nearestHealth != null) {
            log { "go pick ${nearestHealth.posInfo()}" }

            targetPos = nearestHealth.position
        } else if (preferredWeaponToPick != null) {
            log { "go pick ${preferredWeaponToPick!!.posInfo()} instead because we don't want rocket launcher " }
            targetPos = preferredWeaponToPick!!.position
            action.swapWeapon = isClose(targetPos)
        } /*else if (me.weapon?.typ == WeaponType.PISTOL && nearestWeapon != null) {
            log { "go pick ${nearestWeapon.posInfo()} instead pistol " }
            targetPos = nearestWeapon.position
            action.swapWeapon = isClose(targetPos)
        }*/ else if (nearestEnemy != null) {
            log { "go to enemy" }
            //TODO go out from enemy pos, use micro sims?
            targetPos = nearestEnemy.position.copy()
            val mul = if (me.position.x - targetPos.x < 0) -1 else 1
            var distance = me.weapon?.typ?.equals(WeaponType.ROCKET_LAUNCHER).then { 8 } ?: 6
            if (game.currentTick > 2400) {
                distance = 0
            }
            targetPos = targetPos.copy() + Point2D(distance * mul, 0)
        }

        action.aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            if (disableShooting) {
                action.shoot = false
            } else {
                var target = nearestEnemy.center() - me.center()
                if (!nearestEnemy.onLadder && !nearestEnemy.onGround) {
                    val jumpState = nearestEnemy.jumpState
                    if (!jumpState.canCancel && jumpState.maxTime < 1) {
                        target = nearestEnemy.center().minus(0.0, nearestEnemy.size.y / 4) - me.center()
                        log { "aim lower due enemy is falling $nearestEnemy" }
                    }
                }

                val lastWeaponAngle = me.weapon?.lastAngle ?: 0.0

                val angleDiff = lastWeaponAngle - target.angle()
                myPrint { "aim angle diff ${angleDiff.f()}" }
                if (abs(angleDiff) < 0.10) {
                    myPrint { "keep old angle" }
                    target = Point2D(lastWeaponAngle).length(2.0)
                }

                val aims = listOf(target)

                if (canShot(nearestEnemy, aims, action)) {
                    action.shoot = true
                }
            }
        }
        var jump = targetPos.y > me.position.y;
        if (targetPos.x > me.position.x &&
            isObstacleAtDirection(Direction.RIGHT)
        ) {
            jump = true
        }
        if (targetPos.x < me.position.x &&
            isObstacleAtDirection(Direction.LEFT)
        ) {
            jump = true
        }
        if (me.jumpState.canJump.not() && me.onLadder.not()) {
            jump = false
        }
        if (!jump && getMyActions().isNotEmpty()) {
            val lastWasJump = getMyActions().last().jump
            if (!me.onLadder && !me.onGround && lastWasJump) {
                log { "force finish jump" }
                jump = true
            }
        }
        val vectorMove = (me.position.copy() - targetPos).abs()

        if (vectorMove.x < 1.2 && me.position.y > targetPos.y && vectorMove.y > 0.3) {
            jump = false
        }

        log { "me ${me.position} _ target->$targetPos aim=${action.aim}" }
        val travelDistX = targetPos.x - me.position.x
        val travelDistY = targetPos.y - me.position.y

        if (Math.abs(travelDistX) < 0.19 && abs(travelDistY) < 0.2) {
            action.velocity = 0.0
        } else {
            action.velocity = travelDistX * 10000
        }
        action.jump = jump
        if (vectorMove.x < 1) {
            action.jumpDown = jump.not().then { targetPos.y - me.position.y < -0.5f } ?: false
            if (action.jumpDown && isObstacleAtDirection(Direction.DOWN)) {
                val noLeftObstacle = !isObstacleAtDirection(Direction.LEFT)
                val noRightObstacle = !isObstacleAtDirection(Direction.RIGHT)

                if (travelDistX > 0) {
                    if (noRightObstacle) {
                        action.velocity = 9999.0;
                    } else if (noLeftObstacle) {
                        action.velocity = -9999.0;
                    }
                } else {
                    if (noLeftObstacle) {
                        action.velocity = -9999.0;
                    } else if (noRightObstacle) {
                        action.velocity = 9999.0;
                    }
                }
            }
        }
        action.plantMine = false

        fastJumpFix(me, action)

        d { debug.line(me.position, targetPos, ColorFloat.TARGET_POS) }

        return action
    }

    private fun getMyActions() = prevActions.getOrPut(me.id, { mutableListOf() })

    private fun wantToPickRocketLauncher(): Boolean {
        val anotherMe = getAnotherMe() ?: return false

        if (anotherMe.weapon == null) {
            return false
        }

        return me.weapon?.typ != WeaponType.ROCKET_LAUNCHER && anotherMe.weapon?.typ != WeaponType.ROCKET_LAUNCHER
    }

    //TODO rework
    private fun isObstacleAtDirection(dir: Direction): Boolean {
        val walls = game.getTile(me.position, dir, me.size) == Tile.WALL ||
                game.getTile(me.position.copy().plus(0.0, me.size.y), dir, me.size) == Tile.WALL
        walls.then { return true }

        val newPos = me.position.copy().applyDir(dir)
        for (unit in game.units) {
            if (unit != me && unitsCollide(newPos, unit)) {
                return true
            }
        }
        return false
    }

    private fun unitsCollide(newPos: Point2D, unit: Unit): Boolean {
        val distance = newPos.copy().minus(unit.position).abs()
        if (distance.x <= unit.size.x && distance.y <= unit.size.y) {
            return true
        }
        return false
    }

    private fun getHealthPack(ignoreIt: LootBox?): LootBox? {
        val en = getClosestEnemy()
        return game.lootBoxes
            .filter {
                (it.item::class == Item.HealthPack::class).not().then { return@filter false }

                if (it == ignoreIt) {
                    return@filter false
                }

                return@filter !isEnemyCloser(en, it.position)
            }
            .minBy { it.position.pathDist(me.position) }
    }

    private fun isEnemyCloser(en: Unit?, point: Point2D): Boolean {
        if (en == null) {
            return false
        }
        val myDist = me.position.pathDist(point)
        val enDist = en.position.pathDist(point)
        if (myDist < enDist) {
            return false
        }
        val vectorToEn = me.position.copy() - en.position
        val vectorToHeath = me.position.copy() - point
        //if x distance is smaller
        if (abs(vectorToEn.x) < abs(vectorToHeath.x)) {

            //and it is same side
            //ignore health
            if (vectorToEn.x < 0 && vectorToHeath.x < 0) {
                return true
            }
            if (vectorToEn.x > 0 && vectorToHeath.x > 0) {
                return true
            }
        }
        return false
    }

    private fun canShot(target: Unit, aims: List<Point2D>, action: UnitAction): Boolean {
        val center = me.center()
        action.aim = aims.last()

        var canShootOnce = false

        val lastWeaponAngle = me.weapon?.lastAngle ?: 0.0
        val isRocketLauncher = me.weapon?.typ == WeaponType.ROCKET_LAUNCHER

        aims.map { aim ->
            var wallHitPercent = 0.0
            var hitTargetDamage = 0.0
            var hitMeDamage = 0.0

            me.weapon?.let { weapon ->
                val aimAngle = aim.angle()

                val rayCountOneSide = 2  //TODO fix performance somehow
                val stepAngle = weapon.spread / rayCountOneSide

                var stuckCount = 0

                val totalRaysCount = rayCountOneSide * 2 + 1

                repeat(totalRaysCount) { i ->
                    val rayIndex = i - rayCountOneSide
                    val ray = Point2D(aimAngle + rayIndex * stepAngle).length(40 * 1.5)

                    val hitTargetRef = Ref(0.0)
                    val hitMeDamageRef = Ref(0.0)
                    val hitPoint = Ref<Point2D?>(null)

                    val stuckWall =
                        didStuckWithSomething(
                            center.copy(),
                            center.copy() + ray.copy(),
                            hitTargetRef,
                            hitMeDamageRef,
                            target,
                            weapon.params.bullet.size,
                            hitPoint,
                            isRocketLauncher,
                            weapon
                        )

                    if (stuckWall) {
                        stuckCount++
                    }
                    hitTargetDamage += hitTargetRef.ref
                    hitMeDamage += hitMeDamageRef.ref

                    //debug.line(center, center.copy() + ray, ColorFloat.AIM_RAY_FAILED)
                }


                wallHitPercent = stuckCount / totalRaysCount.toDouble()
            }

            AimScore(aim, wallHitPercent, hitTargetDamage, hitMeDamage)
        }.filter {
            if (it.hitTargetDamage <= 0) {
                return false
            }
            val canShoot = if (isRocketLauncher) {
                //game.properties.weaponParams[WeaponType.ROCKET_LAUNCHER]!!.explosion.
                it.hitTargetDamage > it.hitMeDamage * 1.4
            } else {
                it.hitTargetDamage > it.hitMeDamage * 1.4
            }
            myPrint { "canShoot=${game} rocket=${isRocketLauncher} target=${it.hitTargetDamage} me=${it.hitMeDamage}" }
            canShoot
        }.minBy { abs(it.aim.angle().toDouble() - lastWeaponAngle) }
            ?.let {
                canShootOnce = true
                myPrint { "fire at $it" }
            }

        return canShootOnce
    }

    //TODO refactor
    private fun didStuckWithSomething(
        from: Point2D,
        to: Point2D,
        hitTarget: Ref<Double>,
        hitMe: Ref<Double>,
        target: Unit,
        pointSize: Double,
        hitPoint: Ref<Point2D?>,
        isRocketLauncher: Boolean,
        weapon: Weapon
    ): Boolean {
        var pointToCheck = from.copy()

        var weGetWalls = false
        val rayLengthMax = from.distance(to)

        val epsilon = 0.000001

        while (true) {
            // d { debug.rect(pointToCheck, Point2D(0.1, 0.1), ColorFloat.RAY_DIST_CHECK) }


            var distanceTarget = 1000.0
            var closestUnit = target
            for (unit in game.units) {
                if (unit != me) {
                    val dist = signedDist(pointToCheck, unit)
                    if (dist < distanceTarget) {
                        distanceTarget = dist
                        closestUnit = unit
                    }
                }
            }

            var distanceWalls = signedDist(pointToCheck, null)

            distanceWalls -= weapon.params.bullet.size / 2
            distanceTarget -= weapon.params.bullet.size / 2

            val isUnitCloser = distanceTarget < distanceWalls

            var distance = if (isUnitCloser) {
                distanceTarget
            } else {
                distanceWalls
            }

            distance -= pointSize

            if (distance < epsilon) {
                if (isUnitCloser) {
                    closestUnit.isMy().then {
                        hitMe.ref += weapon.params.bullet.damage
                    } ?: run {
                        hitTarget.ref += weapon.params.bullet.damage
                    }
                }
                isRocketLauncher.then {
                    val explosionRadius = weapon.params.explosion!!.radius
                    val damage = weapon.params.explosion!!.damage
                    game.units.forEach { unit ->
                        Simulator.unitAffectedByExplosion(unit, explosionRadius, pointToCheck).then {
                            val distToUnit = unit.position.distance(pointToCheck)
                            if (unit.isMy()) {
                                hitMe.ref += damage;
                                hitMe.ref -= distToUnit / 3f
                            } else {
                                hitTarget.ref += damage;
                                hitMe.ref -= distToUnit / 3f
                            }
                        }
                    }
                }
                weGetWalls = !isUnitCloser
                hitPoint.ref = pointToCheck.copy()
                break
            }

            val remainingVector = to - pointToCheck
            val remainingDist = remainingVector.length()
            if (remainingDist < epsilon) {
                break
            }
            pointToCheck += remainingVector.length(distance)
            if (pointToCheck.distance(from) >= rayLengthMax) {
                break
            }

            //d { debug.circle(pointToCheck, distance, ColorFloat.RAY_DIST_CHECK) }
        }

        d {
            val endFinal = hitPoint.ref ?: to

            val color =
                (hitTarget.ref > hitMe.ref).then { ColorFloat.AIM_RAY_GOOD }
                    ?: (hitTarget.ref < hitMe.ref).then { ColorFloat.AIM_RAY_BAD }
                    ?: weGetWalls.then { ColorFloat.AIM_RAY_WALSS } ?: ColorFloat.AIM_RAY_UNKNOWN

            debug.rect(endFinal, Point2D(0.1, 0.1), color)
            debug.line(from, endFinal, color)
        }
        return weGetWalls
    }

    private fun getAnotherMe(): Unit? {
        return game.units.firstOrNull { it.id != me.id && it.playerId == me.playerId }
    }

    private inline fun myPrint(function: () -> String) {
        log { function() }
    }

    private fun isClose(targetPos: Point2D) = targetPos.distMe() < 1

    private fun <T : Any> getClosestItem(type: KClass<T>): LootBox? {
        return game.lootBoxes.filter { it.item::class == type }.minBy { it.position.pathDist(me.position) }
    }

    private fun wantSwapFromRocketLauncher(me: Unit): Boolean {
        getAnotherMe()?.let {
            if (it.weapon == null) {
                return false
            }
        }
        val isRocketLauncher = me.weapon?.typ == WeaponType.ROCKET_LAUNCHER
        if (!isRocketLauncher) {
            return false
        }
        val anotherMe = getAnotherMe()

        if (anotherMe?.weapon != null && anotherMe.weapon?.typ != WeaponType.ROCKET_LAUNCHER) {
            return false
        }

        val en = getClosestEnemy()

        getPrefferedWeapon(en) ?: return false

        return true
    }

    private fun getPrefferedWeapon(en: Unit?) =
        getClosestWeaponItem(listOf(WeaponType.PISTOL, WeaponType.ASSAULT_RIFLE), en)

    private fun getClosestWeaponItem(types: List<WeaponType>, en: Unit?): LootBox? {
        return game.lootBoxes.filter {
            val item = it.item
            item is Item.Weapon && types.contains(item.weaponType) && !isEnemyCloser(en, it.position)
        }.minBy { it.position.pathDist(me.position) }
    }


    private fun wantSwapToRocketLauncher(me: Unit): Boolean {
        val b = me.weapon?.typ != WeaponType.ROCKET_LAUNCHER
        if (!b) {
            return false
        }
        val rocket = getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER) ?: return false

        return rocket.position.distance(me.position) < getClosestEnemy()?.position?.distance(me.position) ?: 10.0
    }

    override fun toString(): String {
        return "SmartGuyStrategy(disableShooting=$disableShooting)"
    }

}