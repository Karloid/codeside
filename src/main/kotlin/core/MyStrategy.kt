package core

import Debug
import model.*
import model.Unit
import sim.EvalAndSim
import sim.Simulator
import strats.*
import util.f
import util.fori
import util.then
import java.awt.Color
import java.lang.Math.abs
import java.util.*

//TODO calc two sims at once
//TODO Handle two units at once
//TODO highier score to keep jumping
//TODO calc trampline
//TODO better resource management, keep to center of the map
//TODO draw reload
//TODO reload time in eval

//TODO stay away from rocket launcher  (check space around)
//TODO astar way search
class MyStrategy : AbstractStrategy() {

    private var simTill: Int = 0
    private var statBox = StatBox()
    private var prevGame: Game? = null

    private var timeEnd: Long = 0L
    private var timeStart: Long = 0L

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
        timeStart = System.currentTimeMillis()

        val action: UnitAction
        if (game.bullets.isNotEmpty()) {
            simTill =
                (game.currentTick + game.properties.weaponParams[WeaponType.PISTOL]!!.reloadTime * game.properties.ticksPerSecond).toInt()
        }

        if (game.currentTick - simTill > 400) {
            simTill = game.currentTick + 60
        }

        val shootAction = shootingStart.getAction(me, game, debug)

        val noWeapon = me.weapon != null
        var isDoSim = simTill >= game.currentTick && noWeapon

        if (!shootAction.shoot && game.bullets.isEmpty()) {
            isDoSim = false
        }

        if (!isDoSim) {
            action = shootAction
        } else {
            action = doSimMove()

            action.shoot = shootAction.shoot  //TODO enable
            action.aim = shootAction.aim
            action.reload = shootAction.reload
        }
        timeEnd = System.currentTimeMillis()

        printAction(action)
        printMap(action)

        d { debug.draw(CustomData.Log("shoot=${action.shoot} aim=${action.aim}")) }

        prevActions.add(action)

        calcStats()

        return action
    }

    private fun bulletsNear(): Boolean {
        return game.bullets.any { bullet -> bullet.playerId != me.playerId && bullet.position.distance(me.position) < 9 }
    }

    private fun calcStats() {
        if (me.id != game.units.filter { it.isMy() }.minBy { it.id }!!.id) {
            return
        }
        statBox.put(game)

        if (game.currentTick % 200 == 0) {
            statBox.print()
        }
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
                        val unitSize = game.properties.unitSize
                        debug.rect(
                            affectedUnit.copy().plus(0.0, unitSize.y / 2), unitSize,
                            ColorFloat.EXPLOSION_UNIT
                        )
                    }
                }

                for (deadUnit in sim.metainfo.deadUnits) {
                    val unitSize = game.properties.unitSize
                    val center = deadUnit.toUnitCenter()
                    debug.rect(
                        center, unitSize,
                        ColorFloat.DEAD_SIM_UNIT
                    )

                    debug.text("X_X", center, ColorFloat.RED)
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
        var tickK = getMaxJumpTicks() / 3 * 3

        //  variants.clear()
        //  variants.add(DebugAndJumpStrategy())
        //  tickK = getMaxJumpTicks() * 20

        val strat = pickBestStrat(variants, tickK)

        strat.isReal = true
        return strat.getAction(me, game, debug)
    }

    private fun pickBestStrat(strats: MutableList<StrategyAdvCombined>, tickK: Double): StrategyAdvCombined {
        val evalAndSims = ArrayList<EvalAndSim>()

        var i = 0
        while (!strats.isEmpty()) {
            val strat = strats[0]

            val simulator = startSimulator(ProxyStrategy(strat), simColors[i % simColors.size], tickK, i == 0)

            val evalAndSim = eval(simulator, strat)

            strats.removeAt(0)

            evalAndSims.add(evalAndSim)

            i++
        }

        //TODO stop if found good enough, add more strats if want better ones
        //TODO compare calculated and actual time touch, if actual time too differs then calc backward moves

        var best = evalAndSims.maxBy { it.score }!!

        log { "picked strat=${best.strat} score=${best.score}" }
        drawDebugSimulator(evalAndSims, best)

        return best.strat
    }

    private fun eval(simulator: Simulator, strat: StrategyAdvCombined): EvalAndSim {

        var score = 0.0
        val myUnits = game.units.filter { it.isMy() }


        val myUnitsSim = simulator.game.units.filter { it.playerId == me.playerId }

        val remainingTeamHealth = myUnitsSim.sumBy { it.health } * 100
        val enHealth = simulator.game.units.filter { !it.isMy() }.sumBy { it.health } * 1
        score -= enHealth

        val diff = myUnitsSim.size - myUnits.size
        score -= diff * 1000

        score = remainingTeamHealth.toDouble()
        val anotherUnit = getAnotherUnit()
        if (strat is MoveStrategy) {
            if (strat.moveUpDown == MoveUpDown.UP && !me.onGround && !me.onLadder && (anotherUnit?.id ?: 0) > me.id) {
                score += 10
            }
        }

        val currentDistToEnemies = game.getMinDistToEnemies(me)
        val simDistToEnemies = simulator.game.getMinDistToEnemies(me)
        if (me.weapon?.typ == WeaponType.ROCKET_LAUNCHER) {
            score -= simDistToEnemies
        }
        anotherUnit?.let { another ->
            val xDist = (another.position.copy() - me.position).abs().x
            if (xDist > 4) {
                val distToAnother = simulator.game.getDist(me, another)
                score -= distToAnother * 5
            }
            if (xDist > 2) {
                return@let
            }

            val delta = simDistToEnemies - currentDistToEnemies
            val sameHealthButIdLower =
                another.health == me.health && another.id > me.id && another.weapon?.typ != WeaponType.ROCKET_LAUNCHER
            val sameHealthButHasRocketLauncher = another.health == me.health &&
                    me.weapon?.typ == WeaponType.ROCKET_LAUNCHER &&
                    another.weapon?.typ != WeaponType.ROCKET_LAUNCHER

            if (another.health < me.health || (sameHealthButIdLower || sameHealthButHasRocketLauncher)) {
                score -= delta * 5
            } else {
                score += delta * 5
            }
            //log { "simDistToEnemies=${simDistToEnemies} ${currentDistToEnemies}" }
        }

        val distToCenter = abs(me.position.x - game.level.tiles.cellsWidth)
        if (distToCenter > game.level.tiles.cellsWidth / 3) {
            //keep center
            simulator.game.getUnitPosNullable(me.id)?.let { mySimPos ->
                score -= (abs(mySimPos.x - game.level.tiles.cellsWidth)) / 100
            }
        }

        if (me.health != game.properties.unitMaxHealth) {
            score -= getMinDistToHealth(simulator.game, me) * 10
        }

        //keep away when reloading
        if (currentDistToEnemies < 4) {
            if (me.weapon?.fireTimer ?: 0.0 > 0.1) {
                score += simDistToEnemies / 2
            }

        }
        //TODO calc in game score


        return EvalAndSim(score, simulator, strat).apply {
            createdAtTick = game.currentTick
        }
    }

    private fun getMinDistToHealth(game: Game, unit: Unit): Double {
        val actualUnit = game.units.firstOrNull { it.id == unit.id } ?: return 0.0

        val enemies = game.units.filter { it.playerId != unit.playerId }

        return game.lootBoxes
            .filter { it.item is Item.HealthPack }
            .map { health ->
                val distToMe = health.position.distance(actualUnit.position)

                if (enemies.map { health.position.distance(it.position) }.any { it < distToMe }) {
                    return@map 999999.0
                }
                distToMe
            }.min() ?: 0.0
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

            sim.microTicks = game.properties.updatesPerTick / 3

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


    private fun printMap(action: UnitAction) {
        d {
            for (unit in game.units) {
                if (unit.isMy() && unit != me) {
                    continue
                }
                var msg = unit.id.toString()
                if (unit == me) {
                    msg += action.shoot.then { "*" } ?: ""
                }
                debug.text(msg, unit.position, ColorFloat.TEXT_ID)

                unit.weapon?.fireTimer?.let {
                    debug.text(it.f(), unit.position.copy().minus(0.0, 1.0), ColorFloat.GRAY)
                    val x = unit.position.x - unit.size.x / 2
                    val y = unit.position.y - 1
                    debug.rect(x, y, x + 0.2f, y + it, ColorFloat.RELOAD)
                }
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
        log { "final act: onGround=${me.onGround} onLadder=${me.onLadder} canJump=${me.jumpState.canJump}-${me.jumpState.maxTime.f()} canCancel=${me.jumpState.canCancel} \naction:$action took ${timeEnd - timeStart}ms" }
    }

    fun Point2D.toUnitCenter(): Point2D {
        return copy().plus(0.0, game.properties.unitSize.y / 2)
    }
}



