import model.Point2D

class AimScore(
    val aim: Point2D,
    val hitWallPercent: Double,
    val hitTargetPercent: Double,
    val hitMePercent: Double
) {
    override fun toString(): String {
        return "AimScore(aim=$aim, wall=${hitWallPercent.f()}, target=${hitTargetPercent.f()} me=${hitMePercent.f()})"
    }
}
