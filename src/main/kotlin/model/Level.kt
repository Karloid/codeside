package model

import util.PlainArray
import util.StreamUtil

val EMPTY = PlainArray(0, 0, { Tile.WALL })

class Level {
    val walls = ArrayList<Point2D>(220)
    val jumpPads = ArrayList<Point2D>(10)
    @JvmField
    var tiles: PlainArray<Tile> = EMPTY

    constructor() {}
    constructor(tiles: PlainArray<Tile>) {
        this.tiles = tiles
    }

    companion object {

        fun readFrom(stream: java.io.InputStream): Level {
            val result = Level()
            val width = StreamUtil.readInt(stream)
            var plainArray: PlainArray<Tile>? = null
            repeat(width) { x ->
                var tilesValue: Array<Tile>
                val height = StreamUtil.readInt(stream)
                if (plainArray == null) {
                    plainArray = PlainArray(width, height) { Tile.EMPTY }
                }

                repeat(height) { y ->
                    val tile = when (StreamUtil.readInt(stream)) {
                        0 -> Tile.EMPTY
                        1 -> Tile.WALL
                        2 -> Tile.PLATFORM
                        3 -> Tile.LADDER
                        4 -> Tile.JUMP_PAD
                        else -> throw java.io.IOException("Unexpected discriminant value")
                    }
                    if (tile == Tile.WALL) {
                        result.walls.add(Point2D(x, y))
                    }
                    if (tile == Tile.JUMP_PAD) {
                        result.jumpPads.add(Point2D(x, y))
                    }
                    plainArray!!.setFast(x, y, tile)
                }
            }
            result.tiles = plainArray!!
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeInt(stream, tiles.cellsWidth)
        repeat(tiles.cellsWidth) { x ->
            StreamUtil.writeInt(stream, tiles.cellsHeight)
            repeat(tiles.cellsHeight) { y ->
                StreamUtil.writeInt(stream, tiles.getFast(x, y).discriminant)
            }
        }
    }

    fun isAir(point2D: Point2D): Boolean {
        try {
            val fastNoRound = tiles.getFastNoRound(point2D)
            return fastNoRound != Tile.WALL && fastNoRound != Tile.JUMP_PAD
        } catch (e: Exception) {
            return false
        }
    }
}
