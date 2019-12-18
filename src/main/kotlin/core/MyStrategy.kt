package core

import Debug
import model.*
import model.Unit
import sim.EvalAndSim
import sim.Simulator
import strats.*
import util.f
import util.fori
import java.awt.Color
import java.util.*
//TODO calc two sims at once
//TODO Handle two units at once
//TODO highier score to keep jumping
//TODO calc trampline
class MyStrategy : AbstractStrategy() {

    private var prevGame: Game? = null

    private var end: Long = 0L
    private var start: Long = 0L

    private var shootingStart = SmartGuyStrategy(this).apply {
        isReal = true
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

        val action: UnitAction
        if (game.bullets.isEmpty()) {
            action = shootingStart.getAction(me, game, debug)
        } else {
            action = doSimMove()

            val shootAction = shootingStart.getAction(me, game, debug)

            action.shoot = shootAction.shoot  //TODO enable
            action.aim = shootAction.aim
            action.reload = shootAction.reload
        }
        end = System.currentTimeMillis()

        printAction(action)
        printMap()

        d { debug.draw(CustomData.Log("shoot=${action.shoot} aim=${action.aim}")) }

        prevActions.add(action)

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

    private fun drawDebugSimulator(evalAndSims: ArrayList<EvalAndSim>, best: EvalAndSim) {
        var colorIndex = 0
        val smallSize = Point2D(1 / 10f, 1 / 10f)
        val bigSize = smallSize.copy().mul(2.0)

        evalAndSims.forEach { evalAndSim ->
            var myColor = simColors[colorIndex % simColors.size]
            var size = smallSize
            val isBest = best == evalAndSim
            if (isBest) {
                myColor = myColor.copy(a = 1f)
                size = bigSize
            }

            val sim = evalAndSim.simulator

            for (entry in sim.metainfo.movements.entries) {
                entry.value.fori {
                    debug.rect(it, size, myColor)
                }
                colorIndex++
            }

            if (isBest) {
                for (bullet in sim.metainfo.bulletsHistory) {
                    val bulletSize = Point2D(bullet.size, bullet.size)
                    debug.rect(bullet.position, bulletSize, myColor)
                }

                for (hit in sim.metainfo.unitHitRegs) {
                    val size = game.properties.mineSize
                    debug.rect(hit, size, ColorFloat(Color.RED))
                }

                for (exp in sim.metainfo.explosions) {

                    debug.rect(exp.point, Point2D(exp.radius * 2, exp.radius * 2), ColorFloat.EXPLOSION)
                    for (affectedUnit in exp.affectedUnits) {
                        debug.rect(affectedUnit, game.properties.unitSize, ColorFloat.EXPLOSION_UNIT)
                    }
                }
            }
        }
    }

    private fun doSimMove(): UnitAction {
        val variants = mutableListOf<StrategyAdvCombined>()
        val smartGuy = SmartGuyStrategy(this)
        smartGuy.disableShooting = true
        variants.add(smartGuy)
        MoveLeftRight.cValues.forEach { leftRight ->
            MoveUpDown.cValues.forEach { upDown ->
                variants.add(MoveStrategy(leftRight, upDown))
            }
        }

        //   variants.clear()
        //   variants.add(EmptyStrategy(this))

        val strat = pickBestStrat(variants, getMaxJumpTicks() / 3 * 2)

        strat.isReal = true
        return strat.getAction(me, game, debug)
    }

    private fun pickBestStrat(strats: MutableList<StrategyAdvCombined>, tickK: Double): StrategyAdvCombined {
        val evalAndSims = ArrayList<EvalAndSim>()

        var i = 0
        var goals = 0
        while (!strats.isEmpty()) {
            val strat = strats[0]

            val simulator = startSimulator(ProxyStrategy(strat), simColors[i % simColors.size], tickK, i == 0)

            val evalAndSim = eval(simulator, strat)

            strats.removeAt(0)

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

        log { "picked strat=${best.strat} score=${best.score}" }
        drawDebugSimulator(evalAndSims, best)

        return best.strat
    }

    private fun eval(simulator: Simulator, strat: StrategyAdvCombined): EvalAndSim {

        var score = 0.0

        val remainingTeamHealth = simulator.game.units.filter { it.playerId == me.playerId }.sumBy { it.health }

        score = remainingTeamHealth.toDouble()

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

        val simTickCount = (1 * tickK).toInt()
        for (tick in 0..simTickCount) {
            predictStratMoves(myStrat, sim, tick, true, simGame)
            predictStratMoves(enStrat, sim, tick, false, simGame)

            sim.microTicks = game.properties.updatesPerTick / 2

            sim.tick()

            simGame.currentTick++

            val resultGoal = sim.resultKill
            if (resultGoal != null) {
                break
            }
        }
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
            for (unit in game.units) {
                debug.text(unit.id.toString(), unit.position, ColorFloat.TEXT_ID)
            }
        }
    }

    private val simColors = arrayOf(
        ColorFloat(1f, 0.4f, 0.4f, 0.4f),
        ColorFloat(0.4f, 1f, 0.4f, 0.4f),
        ColorFloat(0.4f, 0.4f, 1f, 0.4f),
        ColorFloat(0.4f, 1f, 1f, 0.4f),
        ColorFloat(1f, 1f, 0.4f, 0.4f)
    )

    private inline fun printAction(action: UnitAction) {
        if (!isReal) {
            return
        }
        log { "final act: onGround=${me.onGround} onLadder=${me.onLadder} canJump=${me.jumpState.canJump}-${me.jumpState.maxTime.f()} canCancel=${me.jumpState.canCancel} \naction:$action took ${end - start}ms" }
    }
}


