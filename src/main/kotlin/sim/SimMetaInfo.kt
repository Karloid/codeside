package sim

import model.Bullet
import model.Point2D

class SimMetaInfo {
    val deadUnits = mutableListOf<Point2D>()
    val explosions = mutableListOf<Explosion>()
    val unitHitRegs = mutableListOf<Point2D>()
    val bulletsHistory = ArrayList<Bullet>()
    val movements = mutableMapOf<Int, MutableList<Point2D>>()
}
