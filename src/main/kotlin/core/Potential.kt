package core

import model.Game
import model.Level
import util.PlainArray

object Potential {
    private lateinit var level: Level
    lateinit var potential: PlainArray<Double>


    fun init(game: Game) {
        level = game.level
        calcPotential()
    }

    private fun calcPotential() {
        

    }

}