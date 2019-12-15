package strats

import Debug
import MainKt
import core.AimScore
import core.MyStrategy
import model.*
import model.Unit
import util.Direction
import util.Ref
import util.f
import util.then
import kotlin.math.abs
import kotlin.reflect.KClass

class SmartGuyStrategy(myStrategy: MyStrategy) : AbstractStrategy() {

    var disableShooting: Boolean = false

    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        super.getAction(unit, game, debug)

        return doSmartGuy()
    }

    private fun doSmartGuy(): UnitAction {
        val action = UnitAction()

        val nearestEnemy: Unit? = getClosestEnemy()
        val nearestWeapon = getClosestItem(Item.Weapon::class)
        var nearestHealth = getHealthPack()

        if (me.health == game.properties.unitMaxHealth && game.currentTick > 1300 && nearestHealth != null) {
            nearestHealth = null
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
                val aims = listOf(nearestEnemy.center() - me.center())

                if (canShot(nearestEnemy, aims, action)) {
                    action.shoot = true
                }
            }
        }
        var jump = targetPos.y > me.position.y;
        if (targetPos.x > me.position.x &&
            (game.getTile(me.position, Direction.RIGHT) == Tile.WALL ||
                    game.getTile(me.position.copy().applyDir(Direction.DOWN), Direction.RIGHT) == Tile.WALL)
        ) {
            jump = true
        }
        if (targetPos.x < me.position.x &&
            (game.getTile(me.position, Direction.LEFT) == Tile.WALL ||
                    game.getTile(me.position.copy().applyDir(Direction.DOWN), Direction.LEFT) == Tile.WALL)
        ) {
            jump = true
        }
        if (me.jumpState.canJump.not() && me.onLadder.not()) {
            jump = false
        }
        if (!jump && prevActions.isNotEmpty()) {
            val lastWasJump = prevActions.last().jump
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

        if (Math.abs(travelDistX) < 0.49 && abs(travelDistY) < 0.2) {
            action.velocity = 0.0
        } else {
            action.velocity = travelDistX * 10000
        }
        action.jump = jump
        if (vectorMove.x < 1) {
            action.jumpDown = jump.not().then { targetPos.y - me.position.y < -0.5f } ?: false
        }
        action.plantMine = false

        d { debug.line(me.position, targetPos, ColorFloat.TARGET_POS) }

        return action
    }

    private fun getHealthPack(): LootBox? {
        val en = getClosestEnemy()
        return game.lootBoxes
            .filter {
                (it.item::class == Item.HealthPack::class).not().then { return@filter false }

                return@filter !isEnemyCloser(en, it.position)
            }
            .minBy { it.position.distance(me.position) }
    }

    private fun isEnemyCloser(en: Unit?, point: Point2D): Boolean {
        if (en == null) {
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
            var targetHitPercent = 0.0
            var meHitPercent = 0.0

            me.weapon?.let { weapon ->
                val aimAngle = aim.angle()

                val rayCountOneSide = 1  //TODO fix performance somehow
                val stepAngle = weapon.spread / rayCountOneSide

                var stuckCount = 0
                var hitTargetCount = 0
                var hitMeCount = 0

                val totalRaysCount = rayCountOneSide * 2 + 1

                repeat(totalRaysCount) { i ->
                    val rayIndex = i - rayCountOneSide
                    val ray = Point2D(aimAngle + rayIndex * stepAngle).length(40 * 1.5)

                    val hitTarget = Ref(false)
                    val hitMe = Ref(false)
                    val hitPoint = Ref<Point2D?>(null)

                    val stuckWall =
                        didStuckWithSomething(
                            center.copy(),
                            center.copy() + ray.copy(),
                            hitTarget,
                            hitMe,
                            target,
                            weapon.params.bullet.size,
                            hitPoint,
                            isRocketLauncher,
                            weapon
                        )

                    if (stuckWall) {
                        stuckCount++
                    }
                    if (hitTarget.ref) {
                        hitTargetCount++
                    }
                    if (hitMe.ref) {
                        hitMeCount++
                    }

                    //debug.line(center, center.copy() + ray, ColorFloat.AIM_RAY_FAILED)
                }


                wallHitPercent = stuckCount / totalRaysCount.toDouble()
                targetHitPercent = hitTargetCount / totalRaysCount.toDouble()
                meHitPercent = hitMeCount / totalRaysCount.toDouble()
            }

            AimScore(aim, wallHitPercent, targetHitPercent, meHitPercent)
        }.filter {
            if (isRocketLauncher) {
                it.hitTargetPercent > 0.3f && it.hitTargetPercent >= it.hitMePercent
            } else {
                it.hitTargetPercent > 0.1f && it.hitTargetPercent >= it.hitMePercent
            }
        }.minBy { abs(it.aim.angle().toDouble() - lastWeaponAngle) }
            ?.let {
                val angleDiff = lastWeaponAngle - it.aim.angle()
                myPrint { "aim angle diff ${angleDiff.f()}" }
                if (abs(angleDiff) < 0.05) {
                    myPrint { "keep old angle" }
                    action.aim = Point2D(lastWeaponAngle).length(2.0)
                } else {
                    action.aim = it.aim
                }
                canShootOnce = true
                myPrint { "fire at $it" }
            }

        return canShootOnce
    }

    //TODO refactor
    private fun didStuckWithSomething(
        from: Point2D,
        to: Point2D,
        hitTarget: Ref<Boolean>,
        hitMe: Ref<Boolean>,
        target: Unit,
        pointSize: Double,
        hitPoint: Ref<Point2D?>,
        rocketLauncher: Boolean,
        weapon: Weapon
    ): Boolean {
        var pointToCheck = from.copy()

        var weGetWalls = false
        val rayLengthMax = from.distance(to)

        val epsilon = 0.0001

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

            val distanceWalls = signedDist(pointToCheck, null)

            var isTargetCloser = distanceTarget < distanceWalls

            var distance = if (isTargetCloser) {
                distanceTarget
            } else {
                distanceWalls
            }

            distance -= pointSize

            if (distance < epsilon) {
                var targetAffected = false
                var myAffected = false
                rocketLauncher.then {
                    val explosionRadius = weapon.params.explosion!!.radius
                    targetAffected = isRocketAffected(target, pointToCheck, explosionRadius)


                    myAffected = isRocketAffected(me, pointToCheck, explosionRadius)
                    //TODO koeff instead of boolean
                    if (!myAffected) {
                        myAffected =
                            getAnotherMe()?.let { isRocketAffected(it, pointToCheck, explosionRadius) } ?: false
                    }
                }
                weGetWalls = !isTargetCloser
                hitTarget.ref = targetAffected || (isTargetCloser && !closestUnit.isMy())
                hitMe.ref = myAffected || (isTargetCloser && !closestUnit.isMy())
                hitPoint.ref = (pointToCheck.copy())
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

        /*       d {
               val endFinal = hitPoint.ref ?: to

                       val color =
                           hitTarget.ref.then { ColorFloat.AIM_RAY_GOOD } ?: weGetWalls.then { ColorFloat.AIM_RAY_FAILED }
                           ?: ColorFloat.AIM_RAY_MILK

               debug.rect(endFinal, Point2D(0.1, 0.1), color)
                   debug.line(from, endFinal, color)
               }*/
        return weGetWalls
    }

    private fun getAnotherMe(): Unit? {
        return game.units.firstOrNull { it.id != me.id && it.playerId == me.playerId }
    }

    private inline fun myPrint(function: () -> String) {
        if (false) {
            MainKt.myLog(function())
        }
    }

    private fun isClose(targetPos: Point2D) = targetPos.distMe() < 1

    private fun <T : Any> getClosestItem(type: KClass<T>): LootBox? {
        return game.lootBoxes.filter { it.item::class == type }.minBy { it.position.distance(me.position) }
    }

    private fun wantSwapFromRocketLauncher(me: Unit): Boolean {
        val isRocketLauncher = me.weapon?.typ == WeaponType.ROCKET_LAUNCHER
        if (!isRocketLauncher) {
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
        }.minBy { it.position.distance(me.position) }
    }


    private fun wantSwapToRocketLauncher(me: Unit): Boolean {
        val b = me.weapon?.typ != WeaponType.ROCKET_LAUNCHER
        if (!b) {
            return false
        }
        val rocket = getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER) ?: return false

        return rocket.position.distance(me.position) < getClosestEnemy()?.position?.distance(me.position) ?: 10.0
    }

}