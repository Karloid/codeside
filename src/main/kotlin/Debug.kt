import model.*
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

    fun rect(center: Point2D, size: Point2D, color: ColorFloat) {
        draw(CustomData.Rect(center.copy().plus(-size.x / 2, -size.y / 2).toFloat(), size.toFloat(), color))
    }

    fun circle(center: Point2D, radius: Double, color: ColorFloat) {
        val count = 16

        draw(CustomData.Polygon(Array(count) { i ->
            val point = Point2D((Math.PI * 2) / count * i).length(radius).plus(center).toFloat()
            ColoredVertex(point, color)
        }))
    }

    fun text(
        msg: String,
        position: Point2D,
        colorFloat: ColorFloat,
        size: Float = 20f,
        alignment: TextAlignment = TextAlignment.CENTER
    ) {
        draw(CustomData.PlacedText(msg, position.toFloat(), alignment, size, colorFloat))
    }
}
