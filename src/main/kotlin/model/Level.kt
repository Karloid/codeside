package model

import PlainArray
import util.StreamUtil

class Level {
    lateinit var tiles: PlainArray<Tile>

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
                    val tilesValueValue = when (StreamUtil.readInt(stream)) {
                        0 -> Tile.EMPTY
                        1 -> Tile.WALL
                        2 -> Tile.PLATFORM
                        3 -> Tile.LADDER
                        4 -> Tile.JUMP_PAD
                        else -> throw java.io.IOException("Unexpected discriminant value")
                    }
                    plainArray!!.setFast(x, y, tilesValueValue)
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
}
