import model.Point2D

class AimScore(val aim: Point2D, val wallHitPercent: Double, val targetHitPercent: Double) {
    override fun toString(): String {
        return "AimScore(aim=$aim, wall=${wallHitPercent.f()}, target=${targetHitPercent.f()})"
    }
}
