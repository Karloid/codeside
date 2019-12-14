package core

import Debug
import model.*
import model.Unit
import sim.EvalAndSim
import sim.Simulator
import strats.*
import util.Direction.DOWN
import util.fori
import util.then
import java.util.*

class MyStrategy : AbstractStrategy() {

    private var prevGame: Game? = null

    private var end: Long = 0L
    private var start: Long = 0L

    private var firstDebugSimulator: Simulator? = null

    private var shootingStart = SmartGuyStrategy(this).apply {
        disableShooting = false
    }

    init {
        isReal = true
    }

    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        super.getAction(me, game, debug)
        checkPrevGame()

        log { "jumpInfo=${me.jumpState.description()}" }
        start = System.currentTimeMillis()

        val action = doSimMove()

        val shootAction = shootingStart.getAction(me, game, debug)
        
        action.shoot = shootAction.shoot
        action.aim = shootAction.aim
        action.reload = shootAction.reload

        end = System.currentTimeMillis()

        printAction(action)
        printMap()
        debug.draw(CustomData.Log("shoot=${action.shoot} aim=${action.aim}"))

        prevActions.add(action)

        drawDebugSimulator()
        return action
    }

    private fun checkPrevGame() {
        prevGame?.let {
            for (oldUnit in it.units) {
                game.units.firstOrNull { it.id == oldUnit.id }
                    ?.let { newUnit ->
                        if (newUnit.stand) {

                        }
                    }
            }
        }
        prevGame = game

    }

    private fun drawDebugSimulator() {
        var colorIndex = 0

        firstDebugSimulator?.let { sim ->
            val size = Point2D(1 / 10f, 1 / 10f)

            for (entry in sim.metainfo.movements.entries) {
                val myColor = colors[colorIndex % colors.size]
                entry.value.fori {
                    debug.rect(it, size, myColor)
                }
                colorIndex++
            }
        }
    }

    private fun doSimMove(): UnitAction {
        val variants = mutableListOf<StrategyAdvCombined>()
        val smartGuy = SmartGuyStrategy(this)
        smartGuy.disableShooting = true
        variants.add(smartGuy)
        variants.add(SimpleMoveStrategy())

        val strat = pickBestStrat(variants, 5.0)
        strat.isReal = true
        return strat.getAction(me, game, debug)
    }

    private fun pickBestStrat(strats: MutableList<StrategyAdvCombined>, tickK: Double): StrategyAdvCombined {
        val evalAndSims = ArrayList<EvalAndSim>()

        var i = 0
        var goals = 0
        while (!strats.isEmpty()) {
            val strat = strats[0]

            val evalAndSim = eval(startSimulator(ProxyStrategy(strat), colors[i % colors.size], tickK, i == 0), strat)

            strats.removeAt(0)

            if (firstDebugSimulator == null) {
                firstDebugSimulator = evalAndSim.simulator
            }
            firstDebugSimulator = evalAndSim.simulator

            //checking actual path
            evalAndSims.add(evalAndSim)

            if (evalAndSim.score > 1000) {
                goals++
                if (goals >= 3) {
                    break
                }
            }

            i++
        }

        //TODO stop if found good enough, add more strats if want better ones
        //TODO compare calculated and actual time touch, if actual time too differs then calc backward moves
        //TODO check movement directly to real point of touch

        var best = evalAndSims.maxBy { it.score }!!


        //logAndDebugViewStrats(evalAndSims, best)

        return best.strat
    }

    private fun eval(simulator: Simulator, strat: StrategyAdvCombined): EvalAndSim {

        var score = 0.0

        return EvalAndSim(score, simulator, strat).apply {
            createdAtTick = game.currentTick
        }
    }


    private fun startSimulator(
        myStrat: Strategy,
        color: ColorFloat,
        tickK: Double,
        calcActualPath: Boolean
    ): Simulator {

        val simGame = game.copy()
        val sim = Simulator(simGame, this)
        val enStrat = EmptyStrategy(this)
        // val enStrat = SmartGuySimple()

        var stopDueTooManyTouchesTick = -1
        var forcedMicroTicks = -1
        //forcedMicroTicks = 2

        val simTickCount = (60 * tickK).toInt()
        for (tick in 0..simTickCount) {
            predictStratMoves(myStrat, sim, tick, true, simGame)
            predictStratMoves(enStrat, sim, tick, false, simGame)

            sim.microTicks = game.properties.updatesPerTick

            sim.tick()

            simGame.currentTick++

            val resultGoal = sim.resultKill
            if (resultGoal != null) {
                break
            }
        }
        log { "sim ticks calced ${sim.ticksCacled}" }

        return sim
    }

    private fun predictStratMoves(
        strat: Strategy,
        sim: Simulator,
        tick: Int,
        isMe: Boolean,
        simGame: Game
    ) {
        simGame.units.forEach { unit ->
            if ((isMe && unit.playerId == me.playerId) || (!isMe && unit.playerId != me.playerId)) {
                //val unitcopy = unit.copy()
                val action = strat.getAction(unit, simGame, debug)

                unit.simAction = action
            }
        }
    }


    private fun printMap() {
        d {
            val underMe = me.position.copy().applyDir(DOWN)
            game.level.tiles.get(underMe)?.let {
                debug.rect(underMe.roundX, underMe.roundY, Point2D(1, 1), ColorFloat.WALL_UNDER_ME)
            }
            game.level.tiles.fori { x, y, t ->
                (t == Tile.WALL).then {

                }
            }
        }
    }

    private val colors = arrayOf(
        ColorFloat(1f, 0f, 0f, 0.5f),
        ColorFloat(0f, 1f, 0f, 0.5f),
        ColorFloat(0f, 0f, 1f, 0.5f),
        ColorFloat(0f, 1f, 1f, 0.5f),
        ColorFloat(1f, 1f, 0f, 0.5f),
        ColorFloat(0.1f, 0.1f, 0.8f, 0.5f)
    )

    private inline fun printAction(action: UnitAction) {
        if (!isReal) {
            return
        }
        log { "final act: onGround=${me.onGround} onLadder=${me.onLadder} canJump=${me.jumpState.canJump} canCancel=${me.jumpState.canCancel} \naction:$action took ${end - start}ms" }
    }
}


