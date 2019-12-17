package sim

import model.Point2D

class Explosion(val point: Point2D, val affectedUnits: MutableList<Point2D>, val radius: Double) {

}
