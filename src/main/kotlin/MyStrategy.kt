import Direction.*
import MainKt.Companion.myLog
import model.*
import model.Unit
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KClass

class MyStrategy : Strategy {

    private lateinit var debug: Debug
    private lateinit var me: Unit
    private lateinit var game: Game

    private var end: Long = 0L
    private var start: Long = 0L

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        start = System.currentTimeMillis()
        this.me = me
        this.game = game
        this.debug = debug

        val action = smartGuy(debug, game, me)

        end = System.currentTimeMillis()
        printAction(action)
        printMap()
        debug.draw(CustomData.Log("shoot=${action.shoot} aim=${action.aim}"))
        return action
    }

    private inline fun printAction(action: UnitAction) {
        myPrint { "action:$action took ${end - start}ms" }
    }

    private inline fun myPrint(function: () -> String) {
        myLog { game.currentTick.toString() + ": " + function() }
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
        if (targetPos.x > me.position.x && game.getTile(me.position, RIGHT) == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < me.position.x && game.getTile(me.position, LEFT) == Tile.WALL) {
            jump = true
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
        action.jumpDown = targetPos.y - me.position.y < -0.5f
        action.plantMine = false

        //debug.circle(me.position, 4.0, ColorFloat.RAY_DIST_CHECK)
        return action
    }

    private fun wantSwapToRocketLauncher(me: Unit): Boolean {
        val b = me.weapon?.typ != WeaponType.ROCKET_LAUNCHER
        if (!b) {
            return false
        }
        val rocket = getClosestWeaponItem(WeaponType.ROCKET_LAUNCHER) ?: return false

        return rocket.position.distance(me.position) < getClosestEnemy()?.position?.distance(me.position) ?: 10.0
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
                it.hitTargetPercent > 0.4f
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

    private fun isRocketAffected(target: Unit, pointToCheck: Point2D, explosionRadius: Double): Boolean {
        val center = target.center()
        return (center - pointToCheck).abs().let {
            it.x <= explosionRadius &&
                    it.y <= explosionRadius
        }
    }

    private fun signedDist(pointToCheck: Point2D, target: Unit?): Double {
        var minDist = Double.MAX_VALUE

        val wallSize = Point2D(0.5, 0.5)

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

    private fun drawAimStuff(aim: Point2D) {
        val center = me.center()

        // d { debug.line(center, center.copy() + aim, ColorFloat.AIM) }
    }

    private fun isClose(targetPos: Point2D) = targetPos.distMe() < 1

    private fun <T : Any> getClosestItem(type: KClass<T>): LootBox? {
        return game.lootBoxes.filter { it.item::class == type }.minBy { it.position.distance(me.position) }
    }

    private fun getClosestWeaponItem(weaponType: WeaponType): LootBox? {
        return game.lootBoxes.filter {
            val item = it.item
            item is Item.Weapon && item.weaponType == weaponType
        }.minBy { it.position.distance(me.position) }
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

    private fun printMap() {
        d {
            val underMe = me.position.copy().applyDir(DOWN)
            game.level.tiles.get(underMe)?.let {
                debug.rect(underMe.roundX, underMe.roundY, Point2D(1, 1), ColorFloat.WALL_UNDER_ME)
            }
            game.level.tiles.fori { x, y, t ->
                (t == Tile.WALL).then {

                }
            }
        }
    }

}


