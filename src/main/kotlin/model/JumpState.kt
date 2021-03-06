package model

import util.StreamUtil

class JumpState {
    var canJump: Boolean = false
    var speed: Double = 0.0
    var maxTime: Double = 0.0
    var canCancel: Boolean = false

    constructor() {}
    constructor(canJump: Boolean, speed: Double, maxTime: Double, canCancel: Boolean) {
        this.canJump = canJump
        this.speed = speed
        this.maxTime = maxTime
        this.canCancel = canCancel
    }

    companion object {

        val EMPTY: JumpState = JumpState(true, 0.0, 0.0, false)

        fun readFrom(stream: java.io.InputStream): JumpState {
            val result = JumpState()
            result.canJump = StreamUtil.readBoolean(stream)
            result.speed = StreamUtil.readDouble(stream)
            result.maxTime = StreamUtil.readDouble(stream)
            result.canCancel = StreamUtil.readBoolean(stream)
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeBoolean(stream, canJump)
        StreamUtil.writeDouble(stream, speed)
        StreamUtil.writeDouble(stream, maxTime)
        StreamUtil.writeBoolean(stream, canCancel)
    }

    fun description(): String {
        return toString()
    }

    override fun toString(): String {
        return "JumpState(canJump=$canJump, speed=$speed, maxTime=$maxTime, canCancel=$canCancel)"
    }

    fun copy(): JumpState {
        return JumpState().also {
            it.canJump = canJump
            it.speed = speed
            it.maxTime = maxTime
            it.canCancel = canCancel
        }
    }
}
