package core

import model.Point2D
import util.f

class AimScore(
    val aim: Point2D,
    val hitWallPercent: Double,
    val hitTargetDamage: Double,
    val hitMeDamage: Double
) {
    override fun toString(): String {
        return "core.AimScore(aim=$aim, wall=${hitWallPercent.f()}, target=${hitTargetDamage.f()} me=${hitMeDamage.f()})"
    }
}
