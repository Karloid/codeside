package model

import core.pathDist
import util.Direction
import util.StreamUtil

class Game {
    var currentTick: Int = 0
    @JvmField
    var properties: Properties = Properties.EMPTY
    @JvmField
    var level: Level = Level()
    lateinit var players: Array<Player>
    lateinit var units: Array<Unit>
    lateinit var bullets: Array<Bullet>
    lateinit var mines: Array<Mine>
    lateinit var lootBoxes: Array<LootBox>

    constructor() {}
    constructor(
        currentTick: Int,
        properties: Properties,
        level: Level,
        players: Array<Player>,
        units: Array<Unit>,
        bullets: Array<Bullet>,
        mines: Array<Mine>,
        lootBoxes: Array<LootBox>
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
            result.properties = Properties.readFrom(stream)
            result.level = Level.readFrom(stream)
            result.players = Array(StreamUtil.readInt(stream), {
                var playersValue: Player
                playersValue = Player.readFrom(stream)
                playersValue
            })
            result.units = Array(StreamUtil.readInt(stream), {
                var unitsValue: Unit
                unitsValue = Unit.readFrom(stream)
                unitsValue
            })
            result.bullets = Array(StreamUtil.readInt(stream), {
                var bulletsValue: Bullet
                bulletsValue = Bullet.readFrom(stream)
                bulletsValue
            })
            result.mines = Array(StreamUtil.readInt(stream), {
                var minesValue: Mine
                minesValue = Mine.readFrom(stream)
                minesValue
            })
            result.lootBoxes = Array(StreamUtil.readInt(stream), {
                var lootBoxesValue: LootBox
                lootBoxesValue = LootBox.readFrom(stream)
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

    fun getTile(unitPos: Point2D, dir: Direction, unitSize: Point2D): Tile? {
        val newPointToCheck = unitPos.copy().plus(Point2D.getPointByDir(dir).copy().length(unitSize.x))
        return level.tiles.get(newPointToCheck)
    }

    fun copy(): Game {
        return Game().let {
            it.players = Array(players.size) { ind ->
                val unit = players[ind]
                unit.copy()
            }

            it.units = Array(units.size) { ind ->
                val unit = units[ind]
                val robotEntity = unit.copy()
                robotEntity
            }

            it.bullets = Array(bullets.size) { ind ->
                val unit = bullets[ind]
                val bulletNew = unit.copy()
                bulletNew
            }

            it.lootBoxes = Array(lootBoxes.size) { ind ->
                val unit = lootBoxes[ind]
                val bulletNew = unit.copy()
                bulletNew
            }

            it.mines = Array(mines.size) { ind ->
                val mine = mines[ind]
                val mineCopy = mine.copy()
                mineCopy
            }

            it.level = level
            it.currentTick = currentTick
            it.properties = properties

            it
        }
    }

    fun getMinDistToEnemies(unit: Unit): Double? {
        val id = unit.id
        val playerId = unit.playerId

        val unitPosition = getUnitPosNullable(id) ?: return null

        val minDistanceToEnemy = units
            .filter { it.playerId != playerId }
            .minBy { it.position.pathDist(unitPosition) }?.position?.pathDist(unitPosition) ?: 100.0

        return minDistanceToEnemy
    }

    fun getUnitPos(id: Int) = units.firstOrNull { it.id == id }?.position ?: Point2D(0, 0)
    fun getUnitPosNullable(id: Int) = units.firstOrNull { it.id == id }?.position

    fun getDist(me: Unit, another: Unit): Double {
        return getUnitPos(me.id).pathDist(getUnitPos(another.id))
    }

    fun getUnitNullable(me: Unit): Unit? {
        return units.firstOrNull { it.id == me.id }
    }

    fun healthCount(): Int {
        return lootBoxes.count { it.item is Item.HealthPack }
    }

    fun getMinDistToHealth(mySimPos: Point2D): Double? {
        return lootBoxes.filter { it.item is Item.HealthPack }.map { it.position.pathDist(mySimPos) }.min()
    }
}
