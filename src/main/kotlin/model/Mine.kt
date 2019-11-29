package model

import util.StreamUtil

class Mine {
    var playerId: Int = 0
    lateinit var position: model.Point2D
    lateinit var size: model.Point2D
    lateinit var state: model.MineState
    var timer: Double? = null
    var triggerRadius: Double = 0.0
    lateinit var explosionParams: model.ExplosionParams
    constructor() {}
    constructor(playerId: Int, position: model.Point2D, size: model.Point2D, state: model.MineState, timer: Double?, triggerRadius: Double, explosionParams: model.ExplosionParams) {
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
            result.position = model.Point2D.readFrom(stream)
            result.size = model.Point2D.readFrom(stream)
            when (StreamUtil.readInt(stream)) {
            0 ->result.state = model.MineState.PREPARING
            1 ->result.state = model.MineState.IDLE
            2 ->result.state = model.MineState.TRIGGERED
            3 ->result.state = model.MineState.EXPLODED
            else -> throw java.io.IOException("Unexpected discriminant value")
            }
            if (StreamUtil.readBoolean(stream)) {
                result.timer = StreamUtil.readDouble(stream)
            } else {
                result.timer = null
            }
            result.triggerRadius = StreamUtil.readDouble(stream)
            result.explosionParams = model.ExplosionParams.readFrom(stream)
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
}
