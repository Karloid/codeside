import model.ColorFloat
import model.CustomData
import model.Point2D
import model.Vec2Float
import java.io.IOException
import java.io.OutputStream

class Debug(private val stream: OutputStream) {

    fun draw(customData: model.CustomData) {
        try {
            model.PlayerMessageGame.CustomDataMessage(customData).writeTo(stream)
            stream.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun line(from: Point2D, to: Point2D, color: ColorFloat, width: Float = 1 / 20f) {
        draw(CustomData.Line(from, to, width, color))
    }

    fun rect(x: Float, y: Float, x2: Float, y2: Float, color: ColorFloat) {
        draw(CustomData.Rect(Vec2Float(x, y), Vec2Float(kotlin.math.abs(x - x2), kotlin.math.abs(y - y2)), color))
    }

    fun rect(x: Int, y: Int, x2: Int, y2: Int, color: ColorFloat) {
        rect(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), color)
    }

    fun rect(x: Int, y: Int, size: Point2D, color: ColorFloat) {
        draw(CustomData.Rect(Vec2Float(x.toFloat(), y.toFloat()), size.toFloat(), color))
    }
}
