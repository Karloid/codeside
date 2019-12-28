package model

import util.StreamUtil

class Mine {
    var playerId: Int = 0
    lateinit var position: Point2D
    lateinit var size: Point2D
    lateinit var state: MineState
    var timer: Double? = null
    var triggerRadius: Double = 0.0
    lateinit var explosionParams: ExplosionParams

    constructor() {}
    constructor(
        playerId: Int,
        position: Point2D,
        size: Point2D,
        state: MineState,
        timer: Double?,
        triggerRadius: Double,
        explosionParams: ExplosionParams
    ) {
        this.playerId = playerId
        this.position = position
        this.size = size
        this.state = state
        this.timer = timer
        this.triggerRadius = triggerRadius
        this.explosionParams = explosionParams
    }

    companion object {

        fun readFrom(stream: java.io.InputStream): Mine {
            val result = Mine()
            result.playerId = StreamUtil.readInt(stream)
            result.position = Point2D.readFrom(stream)
            result.size = Point2D.readFrom(stream)
            when (StreamUtil.readInt(stream)) {
                0 -> result.state = MineState.PREPARING
                1 -> result.state = MineState.IDLE
                2 -> result.state = MineState.TRIGGERED
                3 -> result.state = MineState.EXPLODED
                else -> throw java.io.IOException("Unexpected discriminant value")
            }
            if (StreamUtil.readBoolean(stream)) {
                result.timer = StreamUtil.readDouble(stream)
            } else {
                result.timer = null
            }
            result.triggerRadius = StreamUtil.readDouble(stream)
            result.explosionParams = ExplosionParams.readFrom(stream)
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeInt(stream, playerId)
        position.writeTo(stream)
        size.writeTo(stream)
        StreamUtil.writeInt(stream, state.discriminant)
        val timer = timer;
        if (timer == null) {
            StreamUtil.writeBoolean(stream, false)
        } else {
            StreamUtil.writeBoolean(stream, true)
            StreamUtil.writeDouble(stream, timer)
        }
        StreamUtil.writeDouble(stream, triggerRadius)
        explosionParams.writeTo(stream)
    }

    fun center(): Point2D {
        return position.copy().plus(0.0, size.y / 2)
    }

    fun copy(): Mine {
        return Mine().let {
            it.playerId = playerId
            it.position = position.copy()
            it.size = size
            it.state = state
            it.timer = timer
            it.triggerRadius = triggerRadius
            it.explosionParams = explosionParams
            it
        }
    }
}
