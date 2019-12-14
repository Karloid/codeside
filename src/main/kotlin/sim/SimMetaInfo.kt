package sim

import model.Bullet
import model.Point2D

class SimMetaInfo {
    val bulletsHistory = ArrayList<Bullet>()
    val movements = mutableMapOf<Int, MutableList<Point2D>>()
}
