import Direction.*
import MainKt.Companion.myLog
import model.*
import model.Unit
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
        val nearestWeapon = getClosest(Item.Weapon::class)
        val nearestHealth = getClosest(Item.HealthPack::class)

        var targetPos: Point2D = me.position

        if (me.weapon == null && nearestWeapon != null) {
            myPrint { "go pick weapon ${nearestWeapon.posInfo()}" }

            targetPos = nearestWeapon.position
        } else if (nearestHealth != null) {
            myPrint { "go pick ${nearestHealth.posInfo()}" }

            targetPos = nearestHealth.position
        } else if (me.weapon?.typ == WeaponType.PISTOL && nearestWeapon != null) {
            myPrint { "go pick ${nearestWeapon.posInfo()} instead pistol " }

            targetPos = nearestWeapon.position
            action.swapWeapon = isClose(targetPos)
        } else if (nearestEnemy != null) {
            myPrint { "go to enemy" }

            targetPos = nearestEnemy.position.copy() - Point2D(-3, 0)
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))

        action.aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            val aims = listOf(
                nearestEnemy.top() - me.center(),
                nearestEnemy.position.copy() - me.center(),
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
        action.velocity = (targetPos.x - me.position.x) * 10000
        action.jump = jump
        action.jumpDown = targetPos.y - me.position.y < -0.5f
        action.plantMine = false

        //debug.circle(me.position, 4.0, ColorFloat.RAY_DIST_CHECK)
        return action
    }

    private fun canShot(target: Unit, aims: List<Point2D>, action: UnitAction): Boolean {
        val center = me.center()
        action.aim = aims.last()

        var canShootOnce = false

        aims.map { aim ->
            var wallHitPercent = 0.0
            var targetHitPercent = 0.0

            me.weapon?.let { weapon ->
                val aimAngle = aim.angle()

                val rayCountOneSide = 6
                val stepAngle = weapon.spread / rayCountOneSide

                var stuckCount = 0
                var targetCount = 0

                val totalRaysCount = rayCountOneSide * 2 + 1

                repeat(totalRaysCount) { i ->
                    val rayIndex = i - rayCountOneSide
                    val ray = Point2D(aimAngle + rayIndex * stepAngle).length(40 * 1.5)

                    val stuckTarget = Ref(false)
                    val hitPoint = Ref<Point2D?>(null)

                    val stuckWall =
                        didStuckWithSomething(
                            center.copy(),
                            center.copy() + ray.copy(),
                            stuckTarget,
                            target,
                            weapon.params.bullet.size,
                            hitPoint
                        )

                    if (stuckWall) {
                        stuckCount++
                    }
                    if (stuckTarget.ref) {
                        targetCount++
                    }

                    //debug.line(center, center.copy() + ray, ColorFloat.AIM_RAY_FAILED)
                }


                wallHitPercent = stuckCount / totalRaysCount.toDouble()
                targetHitPercent = targetCount / totalRaysCount.toDouble()
            }

            AimScore(aim, wallHitPercent, targetHitPercent)
        }.filter {
            if (me.weapon?.typ == WeaponType.ROCKET_LAUNCHER) {
                it.wallHitPercent < 0.2f //TODO goodCalc
            } else {
                it.targetHitPercent > 0.3f
            }
        }.maxBy { it.targetHitPercent }
            ?.let {
                action.aim = it.aim
                myPrint { "fire at $it" }
            }

        return canShootOnce
    }

    private fun didStuckWithSomething(
        from: Point2D,
        to: Point2D,
        weGetTargetRef: Ref<Boolean>,
        target: Unit,
        pointSize: Double,
        hitPoint: Ref<Point2D?>
    ): Boolean {
        var pointToCheck = from.copy()

        var weGetWalls = false
        val rayLengthMax = from.distance(to)

        val epsilon = 0.0001

        while (true) {
            // d { debug.rect(pointToCheck, Point2D(0.1, 0.1), ColorFloat.RAY_DIST_CHECK) }


            val distanceTarget = signedDist(pointToCheck, target)
            val distanceWalls = signedDist(pointToCheck, null)

            val isTargetClosest = distanceTarget < distanceWalls

            var distance = if (isTargetClosest) {
                distanceTarget
            } else {
                distanceWalls
            }

            distance -= pointSize

            if (distance < epsilon) {
                weGetWalls = !isTargetClosest
                weGetTargetRef.ref = isTargetClosest
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
                weGetTargetRef.ref.then { ColorFloat.AIM_RAY_GOOD } ?: weGetWalls.then { ColorFloat.AIM_RAY_FAILED }
                ?: ColorFloat.AIM_RAY_MILK

            debug.rect(endFinal, Point2D(0.1, 0.1), color)
            debug.line(from, endFinal, color)
        }
        return weGetWalls
    }

    private fun signedDist(pointToCheck: Point2D, target: Unit?): Double {
        var minDist = Double.MAX_VALUE

        val wallSize = Point2D(1, 1)

        if (target != null) {
            minDist = signedDstToBox(pointToCheck.copy(), target.center(), target.size)
        } else {
            game.level.walls.fori {
                minDist = minOf(signedDstToBox(pointToCheck.copy(), it, wallSize), minDist)
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


