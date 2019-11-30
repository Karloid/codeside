package model

import util.StreamUtil

class ColorFloat {
    var r: Float = 0.0f
    var g: Float = 0.0f
    var b: Float = 0.0f
    var a: Float = 0.0f

    constructor() {}
    constructor(r: Float, g: Float, b: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    companion object {

        val AIM: ColorFloat = ColorFloat(1f, 0f, 0f, 1f)
        val AIM_SPREAD: ColorFloat = ColorFloat(1f, 0f, 0f, 0.6f)

        fun readFrom(stream: java.io.InputStream): ColorFloat {
            val result = ColorFloat()
            result.r = StreamUtil.readFloat(stream)
            result.g = StreamUtil.readFloat(stream)
            result.b = StreamUtil.readFloat(stream)
            result.a = StreamUtil.readFloat(stream)
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeFloat(stream, r)
        StreamUtil.writeFloat(stream, g)
        StreamUtil.writeFloat(stream, b)
        StreamUtil.writeFloat(stream, a)
    }
}
