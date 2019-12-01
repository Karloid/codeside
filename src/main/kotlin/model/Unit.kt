package model

import util.StreamUtil

class Unit {
    var playerId: Int = 0
    var id: Int = 0
    var health: Int = 0
    lateinit var position: model.Point2D
    lateinit var size: model.Point2D
    lateinit var jumpState: model.JumpState
    var walkedRight: Boolean = false
    var stand: Boolean = false
    var onGround: Boolean = false
    var onLadder: Boolean = false
    var mines: Int = 0
    var weapon: model.Weapon? = null

    constructor() {}
    constructor(
        playerId: Int,
        id: Int,
        health: Int,
        position: model.Point2D,
        size: model.Point2D,
        jumpState: model.JumpState,
        walkedRight: Boolean,
        stand: Boolean,
        onGround: Boolean,
        onLadder: Boolean,
        mines: Int,
        weapon: model.Weapon?
    ) {
        this.playerId = playerId
        this.id = id
        this.health = health
        this.position = position
        this.size = size
        this.jumpState = jumpState
        this.walkedRight = walkedRight
        this.stand = stand
        this.onGround = onGround
        this.onLadder = onLadder
        this.mines = mines
        this.weapon = weapon
    }

    companion object {

        fun readFrom(stream: java.io.InputStream): Unit {
            val result = Unit()
            result.playerId = StreamUtil.readInt(stream)
            result.id = StreamUtil.readInt(stream)
            result.health = StreamUtil.readInt(stream)
            result.position = model.Point2D.readFrom(stream)
            result.size = model.Point2D.readFrom(stream)
            result.jumpState = model.JumpState.readFrom(stream)
            result.walkedRight = StreamUtil.readBoolean(stream)
            result.stand = StreamUtil.readBoolean(stream)
            result.onGround = StreamUtil.readBoolean(stream)
            result.onLadder = StreamUtil.readBoolean(stream)
            result.mines = StreamUtil.readInt(stream)
            if (StreamUtil.readBoolean(stream)) {
                result.weapon = model.Weapon.readFrom(stream)
            } else {
                result.weapon = null
            }
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeInt(stream, playerId)
        StreamUtil.writeInt(stream, id)
        StreamUtil.writeInt(stream, health)
        position.writeTo(stream)
        size.writeTo(stream)
        jumpState.writeTo(stream)
        StreamUtil.writeBoolean(stream, walkedRight)
        StreamUtil.writeBoolean(stream, stand)
        StreamUtil.writeBoolean(stream, onGround)
        StreamUtil.writeBoolean(stream, onLadder)
        StreamUtil.writeInt(stream, mines)
        val weapon = weapon;
        if (weapon == null) {
            StreamUtil.writeBoolean(stream, false)
        } else {
            StreamUtil.writeBoolean(stream, true)
            weapon.writeTo(stream)
        }
    }

    fun center(): Point2D {
        return position.copy().plus(0.0, size.y / 2)
    }

    fun top(): Point2D {
        return position.copy().plus(0.0, size.y)
    }

    fun copy(): Unit {
        val result = Unit()
        result.playerId = playerId
        result.id = id
        result.health = health
        result.position = position
        result.size = size
        result.jumpState = jumpState
        result.walkedRight = walkedRight
        result.stand = stand
        result.onGround = onGround
        result.onLadder = onLadder
        result.mines = mines
        result.weapon = weapon?.copy()

        return result
    }
}
