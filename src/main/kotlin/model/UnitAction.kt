package model

import util.f
import util.StreamUtil

class UnitAction {
    var velocity: Double = 0.0
    var jump: Boolean = false
    var jumpDown: Boolean = false
    var aim: model.Point2D = Point2D.ZERO
    var shoot: Boolean = false
    var reload: Boolean = false
    var swapWeapon: Boolean = false
    var plantMine: Boolean = false
    constructor() {}
    constructor(velocity: Double, jump: Boolean, jumpDown: Boolean, aim: model.Point2D, shoot: Boolean, reload: Boolean, swapWeapon: Boolean, plantMine: Boolean) {
        this.velocity = velocity
        this.jump = jump
        this.jumpDown = jumpDown
        this.aim = aim
        this.shoot = shoot
        this.reload = reload
        this.swapWeapon = swapWeapon
        this.plantMine = plantMine
    }


    companion object {

        fun readFrom(stream: java.io.InputStream): UnitAction {
            val result = UnitAction()
            result.velocity = StreamUtil.readDouble(stream)
            result.jump = StreamUtil.readBoolean(stream)
            result.jumpDown = StreamUtil.readBoolean(stream)
            result.aim = model.Point2D.readFrom(stream)
            result.shoot = StreamUtil.readBoolean(stream)
            result.reload = StreamUtil.readBoolean(stream)
            result.swapWeapon = StreamUtil.readBoolean(stream)
            result.plantMine = StreamUtil.readBoolean(stream)
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeDouble(stream, velocity)
        StreamUtil.writeBoolean(stream, jump)
        StreamUtil.writeBoolean(stream, jumpDown)
        aim.writeTo(stream)
        StreamUtil.writeBoolean(stream, shoot)
        StreamUtil.writeBoolean(stream, reload)
        StreamUtil.writeBoolean(stream, swapWeapon)
        StreamUtil.writeBoolean(stream, plantMine)
    }

    override fun toString(): String {
        return "UnitAction(velocity=${velocity.f()}, jump=$jump, jumpDown=$jumpDown, aim=$aim, shoot=$shoot, swapWeapon=$swapWeapon, plantMine=$plantMine)"
    }
}
