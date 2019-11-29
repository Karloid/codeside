
import util.StreamUtil
import java.io.*
import java.net.Socket
import java.util.*

class Runner @Throws(IOException::class)
internal constructor(host: String, port: Int, token: String) {
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val socket = Socket(host, port)
        socket.tcpNoDelay = true
        inputStream = BufferedInputStream(socket.getInputStream())
        outputStream = BufferedOutputStream(socket.getOutputStream())
        StreamUtil.writeString(outputStream, token)
        outputStream.flush()
    }

    @Throws(IOException::class)
    internal fun run() {
        val myStrategy: Strategy = MyStrategy()
        val debug = Debug(outputStream)
        while (true) {
            val message = model.ServerMessageGame.readFrom(inputStream)
            val playerView = message.playerView ?: break
            val actions = HashMap<Int, model.UnitAction>()
            for (unit in playerView.game.units) {
                if (unit.playerId == playerView.myId) {
                    actions[unit.id] = myStrategy.getAction(unit, playerView.game, debug)
                }
            }
            model.PlayerMessageGame.ActionMessage(actions).writeTo(outputStream)
            outputStream.flush()
        }
    }

    companion object {

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            println("Started")
            val host = if (args.size < 1) "127.0.0.1" else args[0]
            val port = if (args.size < 2) 31001 else Integer.parseInt(args[1])
            val token = if (args.size < 3) "0000000000000000" else args[2]
            Runner(host, port, token).run()
        }
    }
}
