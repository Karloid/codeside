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

            targetPos = nearestEnemy.position
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))

        var aim = Point2D(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = nearestEnemy.position.copy() - me.position

            drawAimStuff(aim)

            if (canShot(nearestEnemy, aim)) {
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
        myPrint { "me ${me.position} _ target->$targetPos" }
        action.velocity = (targetPos.x - me.position.x) * 10000
        action.jump = jump
        action.jumpDown = targetPos.y - me.position.y < -0.5f
        action.aim = aim
        action.plantMine = false


        //debug.circle(me.position, 4.0, ColorFloat.RAY_DIST_CHECK)
        return action
    }

    private fun canShot(unit: Unit, aim: Point2D): Boolean {
        val center = me.center()

        me.weapon?.let {
            val aimAngle = aim.angle()
            val upAimAngle = aimAngle + it.spread
            val downAimAngle = aimAngle - it.spread

            val rayCount = 6
            val stepAngle = it.spread / rayCount
            repeat(rayCount * 2 + 1) { i ->
                val rayIndex = i - rayCount
                val ray = Point2D(aimAngle + rayIndex * stepAngle).length(aim.length())

                rayMarce(center.copy(), center.copy() + ray.copy())
                //debug.line(center, center.copy() + ray, ColorFloat.AIM_RAY_FAILED)
            }

            val upAim = Point2D(upAimAngle).length(aim.length())
            val downAim = Point2D(downAimAngle).length(aim.length())

            //debug.line(center, center.copy() + upAim, ColorFloat.AIM_SPREAD)
            //debug.line(center, center.copy() + downAim, ColorFloat.AIM_SPREAD)
        }

        return false
    }

    private fun rayMarce(from: Point2D, to: Point2D) {
        var pointToCheck = from.copy()

        var weGetSomething = false

        val epsilon = 0.0001

        while (true) {
            d { debug.rect(pointToCheck, Point2D(0.2, 0.2), ColorFloat.POINT_TO_CHECK) }

            val distance = signedDist(pointToCheck)

            if (distance < epsilon) {
                weGetSomething = true
                break
            }

            val remainingVector = from - to
            val remainingDist = remainingVector.length()
            if (remainingDist < epsilon) {
                break
            }
            pointToCheck += remainingVector.length(distance)

            d {debug.circle(pointToCheck, distance, ColorFloat.RAY_DIST_CHECK)}
        }

        d { debug.line(from, to, weGetSomething.then { ColorFloat.AIM_RAY_FAILED } ?: ColorFloat.AIM_RAY_GOOD) }
    }

    private fun signedDist(pointToCheck: Point2D): Double {
        var minDist = Double.MAX_VALUE

        val wallSize = Point2D(1, 1)

        game.level.walls.fori {
            minDist = minOf(signedDstToBox(pointToCheck.copy(), it, wallSize), minDist)
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


