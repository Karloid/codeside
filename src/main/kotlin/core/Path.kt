package core

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
}

