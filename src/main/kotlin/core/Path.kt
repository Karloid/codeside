package core

import MainKt
import model.Game
import model.Point2D
import model.Tile
import util.PlainArray
import java.util.*

fun Point2D.pathDist(b: Point2D): Double {
    return pathDistInt(b).toDouble()
}

fun Point2D.pathDistInt(b: Point2D): Int {
    return Path.getPathDistI(this, b)
}

object Path {

    lateinit var gameTiles: PlainArray<Tile>
    lateinit var cachedAccess: PlainArray<PlainArray<Int>?>


    fun getPathDistI(a: Point2D, b: Point2D): Int {
        val access = cachedAccess.getFastNoRound(a) ?: cachedAccess.getFastNoRound(b)

        if (access != null) {
            return maxOf(access.getFastNoRound(a), access.getFastNoRound(b))
        }

        val newAccess = calcAccess(a)
        cachedAccess.setFastNoRound(a, newAccess)

        return maxOf(newAccess.getFastNoRound(a), newAccess.getFastNoRound(b))
    }

    fun calcAccess(pointFrom: Point2D, blackList: List<Point2D>? = null): PlainArray<Int> {
        val result = PlainArray(cachedAccess.cellsWidth, cachedAccess.cellsHeight) { Int.MAX_VALUE }

        result.setFastNoRound(pointFrom, 0)

        val queueTest = LinkedList<Point2D>();
        queueTest.add(pointFrom)

        while (true) {
            val el = queueTest.pollFirst() ?: break

            val adjacent = getAdjacent(el.intX, el.intY)

            val myVal = result.getFastNoRound(el)
            adjacent.forEach { candidate ->
                if (blackList?.contains(candidate) == true) {
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

    fun getAdjacent(x: Int, y: Int): MutableList<Point2D> {
        val result = mutableListOf<Point2D>()
        gameTiles.getIfNotWall(x - 1, y)?.let { result.add(it) }
        gameTiles.getIfNotWall(x, y - 1)?.let { result.add(it) }
        gameTiles.getIfNotWall(x, y + 1)?.let { result.add(it) }
        gameTiles.getIfNotWall(x + 1, y)?.let { result.add(it) }
        return result
    }

    fun init(game: Game) {
        gameTiles = game.level.tiles
        cachedAccess = PlainArray(gameTiles.cellsWidth, gameTiles.cellsHeight) { null }
    }

    private fun PlainArray<Tile>.getIfNotWall(x: Int, y: Int) =
        get(x, y)?.let {
            if (it != Tile.WALL) {
                Point2D(x + 0.1, y + 0.1)
            } else {
                null
            }
        }

    fun getNextTarget(start: Point2D, end: Point2D, extraSpace: Int): Point2D {
        try {

            var access = cachedAccess.getFastNoRound(end)
            if (access == null) {
                access = calcAccess(end)
                cachedAccess.setFastNoRound(end, access)
            }

            var currentValue = access.getFastNoRound(start) - extraSpace
            var currentPoint = start

            var targetDistance = 0 + extraSpace
            var delta = currentValue - targetDistance
            if (delta > 0) {
                delta -= 2
                delta = maxOf(delta, 0)
            } else {
                delta = 2
                delta = minOf(delta, 0)
            }
            targetDistance += delta

            while (currentValue != targetDistance) {
                getAdjacent(currentPoint.intX, currentPoint.intY)
                val x = currentPoint.intX
                val y = currentPoint.intY
                val dirToZero = currentValue > targetDistance
                if (compare1(access, x - 1, y, currentValue, dirToZero)) {
                    currentPoint = Point2D(x - 1, y)
                } else if (compare1(access, x + 1, y, currentValue, dirToZero)) {
                    currentPoint = Point2D(x + 1, y)
                } else if (compare1(access, x, y - 1, currentValue, dirToZero)) {
                    currentPoint = Point2D(x, y - 1)
                } else if (compare1(access, x, y + 1, currentValue, dirToZero)) {
                    currentPoint = Point2D(x, y + 1)
                } else {
                    MainKt.log { "failed to search way" }
                    return end
                }
                currentValue = access.getFastNoRound(currentPoint)
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

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

