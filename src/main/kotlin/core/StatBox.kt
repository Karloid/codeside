package core

import MainKt
import MainKt.Companion.log
import ifEnabledLog
import model.Game
import model.Unit
import model.Weapon
import model.WeaponType
import util.fori
import util.then

class StatBox {
    val units = mutableMapOf<Int, Stat>()
    fun put(game: Game) {
        if (!MainKt.enabledLog) {
            return
        }
        for (unit in game.units) {
            val stats = units.getOrPut(unit.id, { Stat(unit) })

            unit.weapon?.let { weapon ->
                val weaponStat = stats.weapons.getOrPut(weapon.typ) { WeaponStat(weapon.typ) }

                weaponStat.put(weapon)
            }
        }
    }

    fun print() {
        ifEnabledLog {


            val sortedUnits = units.values.sortedBy { it.id }
            sortedUnits.fori { unit ->
                unit.print()
            }
        }
    }

}

class Stat(unit: Unit) {


    val id = unit.id
    val playerId = unit.playerId

    val weapons = mutableMapOf<WeaponType, WeaponStat>()

    fun print() {
        log { "unit id=$id playerId=$playerId" }
        weapons.values.sortedBy { it.typ }.fori {
            it.print()
        }
    }
}

class WeaponStat(val typ: WeaponType) {
    val spreads = mutableListOf<Double>()
    var shoots = 0
    fun put(weapon: Weapon) {
        spreads.add(weapon.spread)

        weapon.wasShooting.then { shoots++ }
    }

    fun print() {
        log {
            "$typ spreadAvg=${spreads.average()} spreadCount=${spreads.size} shoots=$shoots"
        }
    }
}
