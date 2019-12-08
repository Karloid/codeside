import model.*
import model.Unit
import kotlin.math.abs
import kotlin.reflect.KClass

class SmartGuy(myStrategy: MyStrategy) : AbstractStrategy() {

    override fun getAction(unit: Unit, game: Game, debug: Debug): UnitAction {
        super.getAction(me, game, debug)

        return smartGuy(debug, game, me)
    }

    private fun smartGuy(debug: Debug, game: Game, me: Unit): UnitAction {
        val action = UnitAction()

        val nearestEnemy: Unit? = getClosestEnemy()
        val nearestWeapon = getClosestItem(Item.Weapon::class)
        val nearestHealth = getClosestItem(Item.HealthPack::class)

        var targetPos: Point2D = me.position

        val wantSwapToRocketLauncher = wantSwapToRocketLauncher(me)
        if (wantSwapToRocketLauncher) {
            getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER)?.let {
                action.swapWeapon = isClose(it.position)
            }
        }

        if (me.weapon == null && nearestWeapon != null) {
            myPrint { "go pick weapon ${nearestWeapon.posInfo()}" }

            targetPos = nearestWeapon.position
        } else if (nearestHealth != null) {
            myPrint { "go pick ${nearestHealth.posInfo()}" }

            targetPos = nearestHealth.position
        } else if (wantSwapToRocketLauncher) {
            val rocket = getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER)!!
            myPrint { "go pick ${rocket.posInfo()} instead because we want rocket launcher " }
            targetPos = rocket.position
            action.swapWeapon = isClose(targetPos)
        } else if (me.weapon?.typ == WeaponType.PISTOL && nearestWeapon != null) {
            myPrint { "go pick ${nearestWeapon.posInfo()} instead pistol " }
            targetPos = nearestWeapon.position
            action.swapWeapon = isClose(targetPos)
        } else if (nearestEnemy != null) {
            myPrint { "go to enemy" }
            val mul = if (me.position.x - targetPos.x > 0) -1 else 1
            var distance = me.weapon?.typ?.equals(WeaponType.ROCKET_LAUNCHER).then { 6 } ?: 4
            targetPos = nearestEnemy.position.copy() + Point2D(distance * mul, 0)
        }

        action.aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            val aims = listOf(
                //    nearestEnemy.top() - me.center(),
                //  nearestEnemy.position.copy() - me.center(),
                nearestEnemy.center() - me.center()
            )

            if (canShot(nearestEnemy, aims, action)) {
                action.shoot = true
            }
        }
        var jump = targetPos.y > me.position.y;
        if (targetPos.x > me.position.x && game.getTile(me.position, Direction.RIGHT) == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < me.position.x && game.getTile(me.position, Direction.LEFT) == Tile.WALL) {
            jump = true
        }
        if (me.jumpState.canJump.not() && me.onLadder.not()) {
            jump = false
        }
        if (!jump && prevActions.isNotEmpty()) {
            val lastWasJump = prevActions.last().jump
            if (!me.onLadder && !me.onGround && lastWasJump) {
                myPrint { "force finish jump" }
                jump = true
            }
        }

        myPrint { "me ${me.position} _ target->$targetPos aim=${action.aim}" }
        val travelDistX = targetPos.x - me.position.x
        val travelDistY = targetPos.y - me.position.y

        if (Math.abs(travelDistX) < 0.49 && abs(travelDistY) < 0.2) {
            action.velocity = 0.0
        } else {
            action.velocity = travelDistX * 10000
        }
        action.jump = jump
        action.jumpDown = jump.not().then { targetPos.y - me.position.y < -0.5f } ?: false
        action.plantMine = false

        debug.line(me.position, targetPos, ColorFloat.TARGET_POS)
        return action
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

                val rayCountOneSide = 6
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
                it.hitTargetPercent > 0.1f
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


            val distanceTarget = signedDist(pointToCheck, target)
            val distanceWalls = signedDist(pointToCheck, null)

            var isTargetClosest = distanceTarget < distanceWalls

            var distance = if (isTargetClosest) {
                distanceTarget
            } else {
                distanceWalls
            }

            distance -= pointSize

            if (distance < epsilon) {
                rocketLauncher.then {
                    val explosionRadius = weapon.params.explosion!!.radius
                    val targetAffected = isRocketAffected(target, pointToCheck, explosionRadius)
                    val meAffected = isRocketAffected(me, pointToCheck, explosionRadius)
                    if (targetAffected) {
                        isTargetClosest = true
                    }
                    hitMe.ref = meAffected
                }
                weGetWalls = !isTargetClosest
                hitTarget.ref = isTargetClosest
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

        d {
            val endFinal = hitPoint.ref ?: to

            val color =
                hitTarget.ref.then { ColorFloat.AIM_RAY_GOOD } ?: weGetWalls.then { ColorFloat.AIM_RAY_FAILED }
                ?: ColorFloat.AIM_RAY_MILK

            debug.rect(endFinal, Point2D(0.1, 0.1), color)
            debug.line(from, endFinal, color)
        }
        return weGetWalls
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


    private fun wantSwapToRocketLauncher(me: Unit): Boolean {
        val b = me.weapon?.typ != WeaponType.ROCKET_LAUNCHER
        if (!b) {
            return false
        }
        val rocket = getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER) ?: return false

        return rocket.position.distance(me.position) < getClosestEnemy()?.position?.distance(me.position) ?: 10.0
    }

}