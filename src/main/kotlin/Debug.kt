import model.ColorFloat
import model.CustomData
import model.Point2D
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
}
