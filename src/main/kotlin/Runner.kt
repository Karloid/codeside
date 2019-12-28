import core.MyStrategy
import core.Path
import core.Potential
import model.UnitAction
import strats.SmartGuyStrategy
import strats.Strategy
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
        var firstHandled = false
        while (true) {
            val message = model.ServerMessageGame.readFrom(inputStream)
            val playerView = message.playerView ?: break

            if (!firstHandled) {
                Path.init(playerView.game)
                Potential.init(playerView.game)
            }

            val actions = HashMap<Int, model.UnitAction>()
            for (unit in playerView.game.units) {
                if (unit.playerId == playerView.myId) {

                    kotlin.runCatching {
                        myStrategy.getAction(unit, playerView.game, debug)
                    }
                        .onFailure {
                            MainKt.log { "got failure $it" }
                            it.printStackTrace()
                            val fallbackAct = kotlin.runCatching {
                                SmartGuyStrategy(myStrategy as MyStrategy).getAction(
                                    unit,
                                    playerView.game,
                                    debug
                                )
                            }.getOrNull() ?: UnitAction()

                            actions[unit.id] = fallbackAct
                        }
                        .onSuccess { actions[unit.id] = it }

                    actions.forEach() {  }
                }
            }
            model.PlayerMessageGame.ActionMessage(model.Versioned(actions)).writeTo(outputStream)
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
