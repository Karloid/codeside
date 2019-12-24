package core

import Debug
import model.*
import model.Unit
import sim.SimScore
import sim.Simulator
import strats.*
import util.*
import java.lang.Math.abs
import java.util.*
import kotlin.math.absoluteValue

//TODO calc two sims at once
//TODO Handle two units at once

//TODO better resource management, keep to center of the map

//TODO stay away from rocket launcher  (check space around) - kind of

//TODO calc potential field of danger zones

//TODO go closer when enemy reloading or near it
//TODO go away when enemy ready
//TODO do not use much rocket launcher?
//TODO add simple run to enemy strategy

//TODO aggressive potential fields for health?
//TODO handle mines
//TODO check jumppad is triggered by any intersect
//TODO handle one sim with other preselected sim

//TODO handle weapon pick trhough sim


class MyStrategy : AbstractStrategy() {

    private var forceSimTill: Int = 0
    private var myLastHp = mutableMapOf<Int, Int>()
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

        if (myLastHp.getOrPut(me.id, { game.properties.unitMaxHealth }) > me.health) {
            //taking damage
            forceSimTill = game.currentTick + 40
        }

        if (isRocketBulletsNearBy()) {
            //taking damage
            forceSimTill = game.currentTick + 40
        }

        if (!shootAction.shoot && game.bullets.isEmpty()) {
            isDoSim = false
        }
        if (!isDoSim && noShootTick() > 150 && game.currentTick % 100 > 50) {
            isDoSim = true
        } else if (isDoSim && noShootTick() > 150) {
            isDoSim = false
        }
        if (game.currentTick < forceSimTill) {
            isDoSim = true
        }

        if (me.weapon == null && (getClosestWeaponItem(null)?.position?.distance(me.position) ?: 999.0) < 1) {
            isDoSim = false
        }
        //isDoSim = true //TODO remove
        if (!isDoSim) {
            action = shootAction
        } else {
            action = doSimMove()

            action.shoot = shootAction.shoot  //TODO enable
            action.aim = shootAction.aim
            action.reload = shootAction.reload
        }
        fixStuck(action)

        timeEnd = System.currentTimeMillis()

        printAction(action)
        printMap(action)

        d { debug.draw(CustomData.Log("shoot=${action.shoot} aim=${action.aim}")) }

        prevActions.getOrPut(me.id) { mutableListOf() }.add(action)

        calcStats()
        myLastHp[me.id] = me.health
        return action
    }

    private fun isRocketBulletsNearBy(): Boolean {
        return game.bullets.any { bullet -> bullet.explosionParams != null && me.position.distance(bullet.position) < 20 }
    }

    private fun fixStuck(action: UnitAction) {

    }

    private fun noShootTick(): Int {
        var count = 0
        val actions = prevActions.getOrPut(me.id, { mutableListOf() })
        for (i in actions.size - 1 downTo 0) {
            if (!actions[i].shoot) {
                count++
            } else {
                return count
            }
        }
        return count
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

    private fun drawDebugSimulator(simScores: ArrayList<SimScore>, best: SimScore) {
        var colorIndex = 0
        val smallSize = Point2D(1 / 10f, 1 / 10f)
        val bigSize = smallSize.copy().mul(2.0)

        simScores.sortedBy { it.score }.asReversed().forEach { evalAndSim ->
            var myColor = simColors[colorIndex % simColors.size]
            var size = smallSize
            val isBest = best == evalAndSim
            if (isBest) {
                myColor = myColor.copy(a = 1f)
                size = bigSize
            }

            log { "sim $colorIndex ${evalAndSim}" }

            val sim = evalAndSim.simulator

            val anotherMeId = getAnotherUnit()?.id
            for (entry in sim.metainfo.movements) {
                if (entry.key != anotherMeId && entry.value.isNotEmpty()) {
                    val last = entry.value.last()
                    entry.value.fori { movePoint ->
                        debug.rect(movePoint, size, myColor)
                        if (movePoint === last && me.id == entry.key) {
                            debug.text("${colorIndex}_${evalAndSim.score.f()}", movePoint, myColor)
                        }
                    }
                }
            }
            colorIndex++

            if (isBest) {
                for (bullet in sim.metainfo.bulletsHistory) {
                    val bulletSize = Point2D(bullet.size, bullet.size)
                    debug.rect(bullet.position, bulletSize, myColor)
                }

                for (hit in sim.metainfo.unitHitRegs) {
                    val size = game.properties.mineSize
                    debug.rect(hit.point, size, ColorFloat.RED)
                    debug.text(
                        hit.bullet.weaponType.toString(),
                        hit.point.copy().minus(0.0, 0.5),
                        ColorFloat.RED,
                        size = 18f
                    )
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

        // variants.clear()
        // //variants.add(MoveStrategy(MoveLeftRight.RIGHT, MoveUpDown.STILL))
        // variants.add(MoveStrategy(MoveLeftRight.RIGHT, MoveUpDown.UP))
        // //variants.add(DebugAndJumpStrategy())
        // tickK = getMaxJumpTicks() * 20

        val strat = pickBestStrat(variants, tickK)

        strat.isReal = true
        return strat.getAction(me, game, debug)
    }

    private fun pickBestStrat(strats: MutableList<StrategyAdvCombined>, tickK: Double): StrategyAdvCombined {
        val evalAndSims = ArrayList<SimScore>()

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

        log { "picked strat=${best}" }
        drawDebugSimulator(evalAndSims, best)

        return best.strat
    }

    private fun eval(simulator: Simulator, strat: StrategyAdvCombined): SimScore {

        val simScore = SimScore(0.0, simulator, strat)

        var score = 0.0

        val myUnitsSim = simulator.game.units.filter { it.playerId == me.playerId }

        val remainingTeamHealth = myUnitsSim.sumBy { it.health } * 100
        val enHealth = simulator.game.units.filter { !it.isMy() }.sumBy { it.health } * 1
        score -= enHealth

        val imRocket = me.weapon?.typ == WeaponType.ROCKET_LAUNCHER
        //val myUnits = game.units.filter { it.isMy() }
        // val diff = myUnitsSim.size - myUnits.size
        // score -= diff * 1000

        score += remainingTeamHealth.toDouble()


        checkStrangeScore(score)

        simScore.myHealthBonus = remainingTeamHealth

        val currentDistToEnemies = game.getMinDistToEnemies(me)!!
        val simDistToEnemies = simulator.game.getMinDistToEnemies(me)

        val anotherUnit = getAnotherUnit()
        if (strat is MoveStrategy) {
            // if (strat.moveUpDown == MoveUpDown.UP && !me.onGround && !me.onLadder && (anotherUnit?.id ?: 0) > me.id) {
            //     score += 10
            // }
        }
        val mySimUnitPos = simulator.game.getUnitPosNullable(me.id)
        val mySimUnit = simulator.game.getUnitNullable(me)
        //jump pad
        if (mySimUnit != null && !mySimUnit.onGround && !mySimUnit.onLadder && mySimUnit.jumpState.maxTime > 0 && !mySimUnit.jumpState.canCancel) {
            score -= 15
        }




        if (simDistToEnemies == null) {
            score -= 1
        }

        anotherUnit?.let { another ->
            score -= simulator.metainfo.unitDamage.getOrPut(another.id, { Ref(0.0) }).ref * 5

            val xDist = (another.position.copy() - me.position).abs().x
            if (xDist > 4) {
                val distToAnother = simulator.game.getDist(me, another)
                score -= distToAnother * 5
                checkStrangeScore(score)
            }
            if (xDist > 2) {
                return@let
            }
            if (simDistToEnemies == null) {
                //we dead
                return@let
            }

            val delta = simDistToEnemies - currentDistToEnemies
            val sameHealthButIdLower =
                another.health == me.health && another.id > me.id && another.weapon?.typ != WeaponType.ROCKET_LAUNCHER
            val sameHealthButHasRocketLauncher = another.health == me.health &&
                    me.weapon?.typ == WeaponType.ROCKET_LAUNCHER &&
                    another.weapon?.typ != WeaponType.ROCKET_LAUNCHER

            //sw
            if (another.health < me.health || (sameHealthButIdLower || sameHealthButHasRocketLauncher)) {
                score -= delta * 5
            } else {
                score += delta * 5
            }
            checkStrangeScore(score)
            //log { "simDistToEnemies=${simDistToEnemies} ${currentDistToEnemies}" }
        }

        score -= simulator.metainfo.unitDamage.getOrPut(me.id, { Ref(0.0) }).ref * 10
        checkStrangeScore(score)

        val distToCenter = abs(me.position.x - game.level.tiles.cellsWidth / 2)
        if (distToCenter > game.level.tiles.cellsWidth / 3) {
            //keep center
            mySimUnitPos?.let { mySimPos ->
                score -= (abs(mySimPos.x - game.level.tiles.cellsWidth / 2)) / 100
                checkStrangeScore(score)
            }
        }

        val noShootTick = noShootTick()
        if (noShootTick > 150 && simDistToEnemies != null) {
            score -= simDistToEnemies
            if (noShootTick > 300) {
                score -= simDistToEnemies * 3
            }
            if (noShootTick > 500) {
                score -= simDistToEnemies * 5
            }
        }

        //health
        var likeGoingToHeal = false
        if (me.health < game.properties.unitMaxHealth * 0.9 && simulator.game.healthCount() > 0) {
            val distToHealtRaw = getMinDistToHealth(simulator.game, me)
            if (distToHealtRaw < 120) {
                val distToHealth = distToHealtRaw * 10
                score -= distToHealth
                checkStrangeScore(score)
                likeGoingToHeal = true
            }
        }

        //keep away when reloading
        if (currentDistToEnemies < 9 && simDistToEnemies != null) {
            var reloadThreshold = 0.15
            if (me.weapon?.typ == WeaponType.ROCKET_LAUNCHER) {
                reloadThreshold = 0.35
            }
            if (me.weapon?.fireTimer ?: 0.0 > reloadThreshold) {
                score += simDistToEnemies / 1.5
                checkStrangeScore(score)
            }
        }
        val simMe = simulator.game.getUnitNullable(me)
        simMe?.onLadder?.then {
            score += 5
        }
        simMe?.onGround?.not()?.then {
            val simPosCopy = simMe.position.copy()
            score += 5
            if (game.level.isAir(simPosCopy.applyDir(Direction.DOWN)) && game.level.isAir(simPosCopy.applyDir(Direction.DOWN))) {
                score += 5
            }
        }

        val mySimPos = mySimUnitPos

        val closestEnemy = getClosestEnemy()

        if (me.weapon?.typ != WeaponType.ROCKET_LAUNCHER) {
            //keep away from rocket mans
            closestEnemy?.let {
                game.units.filter {
                    !it.isMy() && it.weapon?.typ == WeaponType.ROCKET_LAUNCHER
                }.forEach {
                    val dist = me.position.pathDist(it.position)
                    if (mySimPos != null && dist < 6) {
                        score += mySimPos.pathDist(closestEnemy.position)

                        checkStrangeScore(score)
                    }
                }
            }
        }

        //keep to enemy without weapon
        if (mySimPos != null && closestEnemy != null && me.weapon != null && closestEnemy.weapon == null) {
            simulator.game.getUnitPosNullable(closestEnemy.id)?.let { enSimPos ->
                score -= mySimPos.pathDist(enSimPos) * 40
            }
        }

        //keep to enemy without health
        if (simMe != null && mySimPos != null && closestEnemy != null && me.weapon != null && closestEnemy.health * 2 < simMe.health) {
            simulator.game.getUnitPosNullable(closestEnemy.id)?.let { enSimPos ->
                score -= mySimPos.pathDist(enSimPos) * 20
            }
        }

        if (simDistToEnemies != null && me.weapon?.typ == WeaponType.ROCKET_LAUNCHER && !likeGoingToHeal) {
            score -= simDistToEnemies * 3
            checkStrangeScore(score)
        }

        //TODO avoid tight places
        if (mySimPos != null && me.weapon != null) {
            val potentialWalls = Potential.potential.getFastNoRound(mySimPos)
            val wallsPenalty = if (likeGoingToHeal) {
                potentialWalls / 2
            } else {
                potentialWalls * 2
            }
            score -= wallsPenalty
            simScore.potentialWallsPenalty = wallsPenalty
            checkStrangeScore(score)
        }


        //keep back from our rocket man
        anotherUnit?.let {
            if (it.weapon?.typ == WeaponType.ROCKET_LAUNCHER && closestEnemy != null && mySimPos != null) {
                if (isBetween(closestEnemy.position, mySimPos, anotherUnit.position)) {
                    return@let
                }
                val distToANother = me.position.pathDist(anotherUnit.position)
                if (distToANother < 4 && anotherUnit.position.pathDist(closestEnemy.position) < 4.5) {
                    score += mySimPos.pathDist(anotherUnit.position) / 2
                    score += mySimPos.pathDist(closestEnemy.position) / 2

                    if (anotherUnit.position.pathDist(closestEnemy.position) < 2) {
                        score += mySimPos.pathDist(anotherUnit.position) / 2
                        score += mySimPos.pathDist(closestEnemy.position) / 2
                    }
                    checkStrangeScore(score)
                }
            }
        }

        //plus pick gun
        if ((me.weapon == null || (imRocket && shootingStart.ignoreRocket)) && mySimPos != null) {
            var closestWeaponItem = getClosestWeaponItem(null)
            if (imRocket) {
                closestWeaponItem = getPrefferedWeapon(null)
            }
            var pathToGun = closestWeaponItem?.position?.pathDist(mySimPos)
            if (pathToGun != null && pathToGun < 100) {
                pathToGun += closestWeaponItem!!.position.distance(mySimPos) % 1
                val pathPenalty = pathToGun * 100
                score -= pathPenalty
                simScore.pathToGunPenalty = pathPenalty
                checkStrangeScore(score)
            }
        }
        if (mySimPos != null) {

            var path = simulator.game.getMinDistToHealth(mySimPos)
            if (path != null && path < 100) {
                path /= 1
                score -= path
                simScore.pathToHealPenalty = path
                checkStrangeScore(score)
            }
        }

        //falling
        if (mySimUnit != null && !mySimUnit.onGround && !mySimUnit.onLadder && mySimUnit.jumpState.maxTime > 0 &&
            !(imRocket && !likeGoingToHeal)
        ) {
            score -= 15
            if (simDistToEnemies != null) {
                score += simDistToEnemies * 5
            }
        }


        //TODO calc in game score
        //TODO antistuck

        return simScore.apply {
            this.score = score
            createdAtTick = game.currentTick
        }
    }

    private fun checkStrangeScore(score: Double) {
        if (score < -1000) {
            val x = 10
        }
    }

    private fun isBetween(center: Point2D, side1: Point2D, side2: Point2D): Boolean {
        val minX = minOf(side1.x, side2.x)
        val maxX = maxOf(side1.x, side2.x)

        val isBetween = center.x > minX && center.x < maxX
        if (isBetween) {
            return true
        }
        val deltaMin = (center.x - minX).absoluteValue
        val deltaMax = (center.x - maxX).absoluteValue

        val deltaX = minOf(deltaMax, deltaMin)
        if (deltaX < 3) {
            val minY = minOf(side1.y, side2.y)
            val maxY = maxOf(side1.y, side2.y)
            return center.y > minY && center.y < maxY
        }

        return isBetween

    }

    private fun getMinDistToHealth(game: Game, unit: Unit): Double {
        val actualUnit = game.units.firstOrNull { it.id == unit.id } ?: return 0.0

        val enemies = game.units.filter { it.playerId != unit.playerId }

        return game.lootBoxes
            .filter { it.item is Item.HealthPack }
            .map { health ->
                val distToMe = health.position.pathDist(actualUnit.position)

                if (enemies.map { health.position.pathDist(it.position) }.any { it < distToMe }) {
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
        val sim = Simulator(simGame)
        val enStrat = EmptyStrategy(this)

        val simTickCount = (1 * tickK).toInt()
        for (tick in 0..simTickCount) {
            predictStratMoves(myStrat, sim, tick, true, simGame)
            predictStratMoves(enStrat, sim, tick, false, simGame)

            sim.microTicks = game.properties.updatesPerTick / 5

            sim.tick()

            simGame.currentTick++

            val resultGoal = sim.resultKill
            if (resultGoal != null) {
                break
            }
        }
        return sim
    }

    private val stillStrategy = MoveStrategy(MoveLeftRight.STILL, MoveUpDown.STILL)

    private fun predictStratMoves(
        strat: Strategy,
        sim: Simulator,
        tick: Int,
        isMe: Boolean,
        simGame: Game
    ) {
        val anotherMe = getAnotherUnit()

        simGame.units.forEach { unit ->
            if ((isMe && unit.playerId == me.playerId) || (!isMe && unit.playerId != me.playerId)) {
                //val unitcopy = unit.copy()

                var action = strat.getAction(unit, simGame, debug)
                if (true) {
                    if (anotherMe?.id == unit.id) {
                        val delta = anotherMe.position.copy().minus(me.position).abs()
                        if (delta.x < 1.5 && delta.y < 2) {
                            action = stillStrategy.getAction(unit, simGame, debug)
                        }
                    }
                }

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
            @Suppress("NullableBooleanElvis")
            //print map
            if (getAnotherUnit()?.let { it.id < me.id } ?: true && false) {
                me.position.pathDist(Point2D(0, 0))
                Path.cachedAccess.getFastNoRound(me.position)?.let { access ->
                    access.fori { x, y, v ->
                        if (v > 120) {
                            return@fori
                        }
                        debug.text("$v", Point2D(x, y), ColorFloat.ACCESS, 18f, alignment = TextAlignment.LEFT)
                    }
                }
            }
            //print passable
            val clossestEnemyPos = getClosestEnemy()?.position
            if (getAnotherUnit()?.let { it.id < me.id } ?: true && true && clossestEnemyPos != null) {

                Path.getNextMoveTarget(me.position, clossestEnemyPos, 0)
                Path.cachedAccessMove.get(clossestEnemyPos)?.let { access ->
                    access.fori { x, y, v ->
                        if (v > 120) {
                            return@fori
                        }
                        debug.text("$v", Point2D(x, y), ColorFloat.ACCESS, 18f, alignment = TextAlignment.LEFT)
                    }
                }
            }

            //print map
            if (getAnotherUnit()?.let { it.id < me.id } ?: true && false) {
                Potential.potential.fori { x, y, v ->
                    if (v < 0.1) {
                        return@fori
                    }
                    debug.text("${v.f1()}", Point2D(x, y), ColorFloat.POTENTIAL, 13f, TextAlignment.LEFT)
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
        log {
            "final act: onGround=${me.onGround} onLadder=${me.onLadder} " +
                    "jumpstate=${me.jumpState.description()} \naction:$action took ${timeEnd - timeStart}ms"
        }
    }

    fun Point2D.toUnitCenter(): Point2D {
        return copy().plus(0.0, game.properties.unitSize.y / 2)
    }
}



