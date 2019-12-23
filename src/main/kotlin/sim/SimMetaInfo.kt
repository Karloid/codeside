package sim

import model.Bullet
import model.Point2D
import util.Ref

class SimMetaInfo {
    val unitDamage = mutableMapOf<Int, Ref<Double>>()
    val deadUnits = mutableListOf<Point2D>()
    val explosions = mutableListOf<Explosion>()
    val unitHitRegs = mutableListOf<BulletHitPoint>()
    val bulletsHistory = ArrayList<Bullet>()
    //id to positions
    val movements = mutableMapOf<Int, MutableList<Point2D>>()
}
