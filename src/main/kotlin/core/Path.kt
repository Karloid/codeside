package core

import MainKt
import model.Game
import model.Point2D
import model.Tile
import util.PlainArray
import util.then
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

fun Point2D.pathDist(b: Point2D): Double {
    return pathDistInt(b).toDouble()
}

fun Point2D.pathDistInt(b: Point2D): Int {
    return Path.getPathDistI(this, b)
}

object Path {

    lateinit var gameTiles: PlainArray<Tile>
    lateinit var cachedAccess: PlainArray<PlainArray<Int>?>
    lateinit var cachedAccessMove: PlainArray<PlainArray<Int>?>
    lateinit var cachedPassable: PlainArray<Boolean?>


    fun getPathDistI(a: Point2D, b: Point2D): Int {
        val access = cachedAccess.getFastNoRound(a) ?: cachedAccess.getFastNoRound(b)

        if (access != null) {
            return maxOf(access.getFastNoRound(a), access.getFastNoRound(b))
        }

        val newAccess = calcAccess(a)
        cachedAccess.setFastNoRound(a, newAccess)

        return maxOf(newAccess.getFastNoRound(a), newAccess.getFastNoRound(b))
    }

    fun calcAccess(
        pointFrom: Point2D,
        blackList: List<Point2D>? = null,
        checkIsPassable: Boolean = false
    ): PlainArray<Int> {
        val maxValue: Int? = Int.MAX_VALUE
        val result = PlainArray(cachedAccess.cellsWidth, cachedAccess.cellsHeight) { maxValue!! }

        result.setFastNoRound(pointFrom, 0)

        val queueTest = LinkedList<Point2D>();
        queueTest.add(pointFrom)

        while (true) {
            val el = queueTest.pollFirst() ?: break

            val adjacent = getAdjacent(el.intX, el.intY)

            val myVal = result.getFastNoRound(el)
            adjacent.forEach { candidate ->
                if (blackList?.contains(candidate) == true || (checkIsPassable && !isPassable(candidate, result))) {
                    return@forEach
                }
                val candidateVal = result.getFastNoRound(candidate)
                if (candidateVal > myVal + 1) {
                    result.setFastNoRound(candidate, myVal + 1)
                    queueTest.add(candidate)
                }
            }
        }

        return result
    }

    fun isOk(candidate1: Point2D): Boolean {
        cachedPassable.getFastNoRound(candidate1)?.let { return it }
        val tile = gameTiles.getFastNoRound(candidate1)
        val result = when (tile) {
            Tile.EMPTY -> {
                val x = candidate1.intX
                var y = candidate1.intY
                val initialY = candidate1.intY

                var result = false
                while (y > -1) {
                    y--
                    val tileBelow = gameTiles.getFast(x, y)
                    val currentDelta = initialY - y
                    if (tileBelow == Tile.WALL || tileBelow == Tile.PLATFORM || tileBelow == Tile.LADDER) {
                        result = currentDelta <= 6
                        break
                    } else if (tileBelow == Tile.JUMP_PAD) {
                        result = currentDelta <= 10
                        break
                    }
                    if (currentDelta > 10) {
                        result = false
                    }
                }
                result
            }
            Tile.WALL -> false
            Tile.PLATFORM -> true
            Tile.LADDER -> true
            Tile.JUMP_PAD -> true
        }

        cachedPassable.setFastNoRound(candidate1, result)
        return result
    }

    private fun isPassable(candidate: Point2D, calcedAccess: PlainArray<Int>): Boolean {
        cachedPassable.getFastNoRound(candidate)?.let { return it }

        if (calcedAccess.getFast(candidate.intX, candidate.intY + 1) != Int.MAX_VALUE) {
            return true
        }
        val left1 = candidate.copy().minus(1.0, 0.0)
        val left1IsNotWall = gameTiles.getFastNoRound(left1) != Tile.WALL
        val left2 = candidate.copy().minus(2.0, 0.0)
        val right1 = candidate.copy().plus(1.0, 0.0)
        val right1IsNotWall = gameTiles.getFastNoRound(right1) != Tile.WALL
        val right2 = candidate.copy().plus(2.0, 0.0)
        val result = isOk(candidate) ||
                isOk(left1) ||
                (left1IsNotWall && isOk(left2)) ||
                isOk(right1) ||
                (right1IsNotWall && isOk(right2))

        cachedPassable.setFastNoRound(candidate, result)
        return result
    }

    val result = ArrayList<Point2D>(4)
    fun getAdjacent(x: Int, y: Int): MutableList<Point2D> {
        result.clear()
        gameTiles.getIfNotWall(x - 1, y)?.let { result.add(it) }
        gameTiles.getIfNotWall(x, y - 1)?.let { result.add(it) }
        gameTiles.getIfNotWall(x, y + 1)?.let { result.add(it) }
        gameTiles.getIfNotWall(x + 1, y)?.let { result.add(it) }
        return result
    }

    fun init(game: Game) {
        gameTiles = game.level.tiles
        cachedAccess = PlainArray(gameTiles.cellsWidth, gameTiles.cellsHeight) { null }
        cachedAccessMove = PlainArray(gameTiles.cellsWidth, gameTiles.cellsHeight) { null }
        cachedPassable = PlainArray(gameTiles.cellsWidth, gameTiles.cellsHeight) { null }
    }

    private inline fun PlainArray<Tile>.getIfNotWall(x: Int, y: Int) =
        get(x, y)?.let {
            if (it != Tile.WALL) {
                Point2D(x + 0.1, y + 0.1)
            } else {
                null
            }
        }

    fun getNextMoveTarget(start: Point2D, end: Point2D, extraSpace: Int): Point2D {
        try {

            var access = cachedAccessMove.getFastNoRound(end)
            if (access == null) {
                access = calcAccess(end, checkIsPassable = true)
                cachedAccessMove.setFastNoRound(end, access)
            }

            var currentValue = access.getFastNoRound(start)
            var currentPoint = start

            var targetValue = 0 + 0
            var initialTargetValue = 0 + 0

            targetValue = maxOf(currentValue - 2, 0)

            currentValue = access.getFastNoRound(currentPoint)

            var finalDelta = abs(currentValue - initialTargetValue)

            val availableVariants = ArrayList<Point2D>(4)
            var count = 0
            while (currentValue != targetValue) {
                count++
                if (count > 80) {
                    MainKt.log { "UNABLE TO FIND PATH IN 80 loop" }
                    return end
                }
                //getAdjacent(currentPoint.intX, currentPoint.intY)
                val x = currentPoint.intX
                val y = currentPoint.intY
                val dirToZero = currentValue > targetValue

                if (compare1(access, x - 1, y, currentValue, dirToZero)) {
                    availableVariants.add(Point2D(x - 1, y))
                }
                if (compare1(access, x + 1, y, currentValue, dirToZero)) {
                    availableVariants.add(Point2D(x + 1, y))
                }
                if (compare1(access, x, y - 1, currentValue, dirToZero)) {
                    availableVariants.add(Point2D(x, y - 1))
                }
                if (compare1(access, x, y + 1, currentValue, dirToZero)) {
                    availableVariants.add(Point2D(x, y + 1))
                }
                if (availableVariants.isEmpty()) {
                    MainKt.log { "failed to search way bad value" }
                    val newValue = access.getFastNoRound(currentPoint)
                    if (currentValue != newValue) {
                        MainKt.log { "restore current value $currentValue -> $newValue" }
                        currentValue = newValue

                        continue
                    }

                    return end
                }
                currentPoint = availableVariants.minBy {
                    when (gameTiles.getFastNoRound(currentPoint)) {
                        Tile.EMPTY -> 5.0
                        Tile.WALL -> 999.0
                        Tile.PLATFORM -> 5.0
                        Tile.LADDER -> 2.5
                        Tile.JUMP_PAD -> (finalDelta > 3).then { 2.5 } ?: 6.0
                    }
                }!!

                currentValue = access.getFastNoRound(currentPoint)
                availableVariants.clear()
            }
            return currentPoint
        } catch (e: Exception) {
            MainKt.log { "failed getNextTarget exception=$e" }
        }
        return end
    }

    private fun compare1(access: PlainArray<Int>, x: Int, y: Int, currentValue: Int, toZero: Boolean): Boolean {
        val valueAtPoint = access.getFast(x, y)
        if (valueAtPoint == Integer.MAX_VALUE) {
            return false
        }
        if (toZero) {
            return valueAtPoint < currentValue
        } else {
            return valueAtPoint > currentValue
        }
    }
}

