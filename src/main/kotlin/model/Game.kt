package model

import Direction
import util.StreamUtil

class Game {
    var currentTick: Int = 0
    lateinit var properties: model.Properties
    lateinit var level: Level
    lateinit var players: Array<model.Player>
    lateinit var units: Array<model.Unit>
    lateinit var bullets: Array<model.Bullet>
    lateinit var mines: Array<model.Mine>
    lateinit var lootBoxes: Array<model.LootBox>

    constructor() {}
    constructor(
        currentTick: Int,
        properties: model.Properties,
        level: Level,
        players: Array<model.Player>,
        units: Array<model.Unit>,
        bullets: Array<model.Bullet>,
        mines: Array<model.Mine>,
        lootBoxes: Array<model.LootBox>
    ) {
        this.currentTick = currentTick
        this.properties = properties
        this.level = level
        this.players = players
        this.units = units
        this.bullets = bullets
        this.mines = mines
        this.lootBoxes = lootBoxes
    }

    companion object {

        fun readFrom(stream: java.io.InputStream): Game {
            val result = Game()
            result.currentTick = StreamUtil.readInt(stream)
            result.properties = model.Properties.readFrom(stream)
            result.level = Level.readFrom(stream)
            result.players = Array(StreamUtil.readInt(stream), {
                var playersValue: model.Player
                playersValue = model.Player.readFrom(stream)
                playersValue
            })
            result.units = Array(StreamUtil.readInt(stream), {
                var unitsValue: model.Unit
                unitsValue = model.Unit.readFrom(stream)
                unitsValue
            })
            result.bullets = Array(StreamUtil.readInt(stream), {
                var bulletsValue: model.Bullet
                bulletsValue = model.Bullet.readFrom(stream)
                bulletsValue
            })
            result.mines = Array(StreamUtil.readInt(stream), {
                var minesValue: model.Mine
                minesValue = model.Mine.readFrom(stream)
                minesValue
            })
            result.lootBoxes = Array(StreamUtil.readInt(stream), {
                var lootBoxesValue: model.LootBox
                lootBoxesValue = model.LootBox.readFrom(stream)
                lootBoxesValue
            })
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeInt(stream, currentTick)
        properties.writeTo(stream)
        level.writeTo(stream)
        StreamUtil.writeInt(stream, players.size)
        for (playersElement in players) {
            playersElement.writeTo(stream)
        }
        StreamUtil.writeInt(stream, units.size)
        for (unitsElement in units) {
            unitsElement.writeTo(stream)
        }
        StreamUtil.writeInt(stream, bullets.size)
        for (bulletsElement in bullets) {
            bulletsElement.writeTo(stream)
        }
        StreamUtil.writeInt(stream, mines.size)
        for (minesElement in mines) {
            minesElement.writeTo(stream)
        }
        StreamUtil.writeInt(stream, lootBoxes.size)
        for (lootBoxesElement in lootBoxes) {
            lootBoxesElement.writeTo(stream)
        }
    }

    fun getTile(position: Point2D, dir: Direction): Tile? {
        val newPos = position.copy().applyDir(dir)
        return level.tiles.get(newPos)
    }
}
