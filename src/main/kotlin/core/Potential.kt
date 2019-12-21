package core

import MainKt
import model.Game
import model.Level
import model.Point2D
import model.Tile
import util.Direction
import util.PlainArray
import util.then

object Potential {
    private lateinit var level: Level
    lateinit var potential: PlainArray<Double>


    fun init(game: Game) {
        level = game.level
        calcPotential()
    }

    private fun calcPotential() {
        fun convert(value: Int): Double {
            if (value > 3) {
                return 0.0
            }
            if (value == 3) {
                return 1.0
            }
            if (value == 2) {
                return 2.0
            }
            if (value == 1) {
                return 4.0
            }
            if (value == 0) {
                return 5.0
            }
            return 5.0
        }

        potential = PlainArray(level.tiles.cellsWidth, level.tiles.cellsHeight) { 0.0 }
        level.tiles.fori { x, y, v ->
            if (v == Tile.WALL) {
                return@fori
            }
            val distRight = getDistToWall(x, y, Direction.RIGHT).let { convert(it) }
            var distTop = getDistToWall(x, y, Direction.UP)?.let { convert(it) }
            val distLeft = getDistToWall(x, y, Direction.LEFT)?.let { convert(it) }
            var distDown = getDistToWall(x, y, Direction.DOWN)?.let { convert(it) }

            distDown /= 4
            distTop /= 1.5

            var count = 0
            if (distRight > 0) {
                count++
            }
            if (distTop > 0) {
                count++
            }
            if (distLeft > 0) {
                count++
            }
            if (distDown > 0) {
                count++
            }

            val multiplayer = count.equals(1).then { 0.5 } ?: count.toDouble()
            val value = (distRight + distTop + distLeft + distDown) * multiplayer

            potential.setFast(x, y, value)

        }
    }

    private fun getDistToWall(x: Int, y: Int, direction: Direction): Int {
        var dist = 0
        try {
            val currentPoint = Point2D(x, y).applyDir(direction)
            while (true) {
                val tile = level.tiles.getNoRound(currentPoint)
                if (tile == null || tile == Tile.WALL) {
                    return dist
                }
                dist++
                currentPoint.applyDir(direction)
            }
        } catch (e: Exception) {
            MainKt.log { "got exception while checking dist $e" }
            return dist
        }
    }
}