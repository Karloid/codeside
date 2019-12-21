package model

import util.StreamUtil
import java.awt.Color

data class ColorFloat(
    var r: Float = 0.0f,
    var g: Float = 0.0f,
    var b: Float = 0.0f,
    var a: Float = 0.0f
) {

    companion object {

        val ACCESS = ColorFloat(Color.WHITE).apply { a = 0.3f }
        val POTENTIAL = ColorFloat(Color.WHITE).apply { a = 0.5f }
        val RELOAD = ColorFloat(Color.ORANGE)
        val GRAY = ColorFloat(Color.PINK)
        val RED: ColorFloat = ColorFloat(Color.RED)
        val ORANGE: ColorFloat = ColorFloat(Color.ORANGE)
        val TEXT_ID = ColorFloat(Color.BLACK)
        val EXPLOSION: ColorFloat = ColorFloat(Color.YELLOW).apply { a = 0.5f }
        val EXPLOSION_UNIT: ColorFloat = ColorFloat(1f, 0.9f, 0f, 0.4f)
        val DEAD_SIM_UNIT: ColorFloat = ColorFloat(Color.PINK).apply { a = 0.5f }
        val TARGET_POS: ColorFloat = ColorFloat(Color.YELLOW).apply { }
        val TARGET_POS_REAL: ColorFloat = ColorFloat(Color.pink).apply { a = 0.7f }
        val POINT_TO_CHECK = ColorFloat(0f, 0f, 1f, 1f)
        val WALL: ColorFloat = ColorFloat(Color.LIGHT_GRAY)
        val WALL_UNDER_ME: ColorFloat = ColorFloat(Color.DARK_GRAY)

        val AIM: ColorFloat = ColorFloat(1f, 0f, 0f, 1f)
        val AIM_SPREAD: ColorFloat = ColorFloat(1f, 0f, 0f, 0.6f)
        val AIM_RAY_GOOD: ColorFloat = ColorFloat(Color.GREEN)
        val AIM_RAY_BAD: ColorFloat = ColorFloat(Color.RED)
        val AIM_RAY_UNKNOWN: ColorFloat = ColorFloat(Color.PINK)
        val AIM_RAY_WALSS: ColorFloat = ColorFloat(1f, 0f, 1f, 0.6f)
        val RAY_DIST_CHECK: ColorFloat = ColorFloat(1f, 1f, 1f, 0.1f)

        fun readFrom(stream: java.io.InputStream): ColorFloat {
            val result = ColorFloat()
            result.r = StreamUtil.readFloat(stream)
            result.g = StreamUtil.readFloat(stream)
            result.b = StreamUtil.readFloat(stream)
            result.a = StreamUtil.readFloat(stream)
            return result
        }
    }

    constructor(color: Color) : this(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeFloat(stream, r)
        StreamUtil.writeFloat(stream, g)
        StreamUtil.writeFloat(stream, b)
        StreamUtil.writeFloat(stream, a)
    }
}
