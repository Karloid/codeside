package model

import Direction
import FastMath
import MainKt
import f
import util.StreamUtil
import kotlin.math.round

class Point2D {
    var x: Double
    var y: Double

    val intX: Int
        get() = x.toInt()

    val intY: Int
        get() = y.toInt()

    val roundX: Int
        get() = round(x).toInt()

    val roundY: Int
        get() = round(y).toInt()

    val fx: Float
        get() = x.toFloat()

    val fy: Float
        get() = y.toFloat()

    constructor(x: Int, y: Int, `val`: Double) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    override fun toString(): String {
        return "x=" + x.f() +
                ", y=" + y.f()
    }

    internal constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun getDistanceTo(x: Double, y: Double): Double {
        return getDistance(this.x, this.y, x, y)
    }

    /*   public static double getDistance(double x1, double y1, double x2, double y2) {
           double dx = x1 - x2;
           double dy = y1 - y2;
           return FastMath.hypot(dx, dy);
       }
   */
    fun getDistanceTo(point: Point2D): Double {
        return getDistanceTo(point.x, point.y)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val point2D = o as Point2D?

        return Integer.compare(point2D!!.intX, intX) == 0 && Integer.compare(point2D.intY, intY) == 0
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(x)
        result = (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(y)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        return result
    }


    fun add(x: Double, y: Double): Point2D {
        return Point2D(this.x + x, this.y + y)
    }

    constructor() {
        x = 0.0
        y = 0.0
    }

    constructor(v: Point2D) {
        this.x = v.x
        this.y = v.y
    }

    constructor(angle: Double) {
        this.x = Math.cos(angle)
        this.y = Math.sin(angle)
    }

    constructor(x: Int, y: Int) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    fun copy(): Point2D {
        return Point2D(this)
    }

    operator fun minus(v: Point2D): Point2D {
        return Point2D(x - v.x, y - v.y)
    }

    fun minus(dx: Double, dy: Double): Point2D {
        return Point2D(x - dx, y - dy)
    }

    fun mul(f: Double): Point2D {
        return Point2D(x * f, y * f)
    }

    fun length(): Double {
        //        return hypot(x, y);
        return FastMath.hypot(x, y)
    }

    fun distance(v: Point2D): Double {

        //        return hypot(x - v.x, y - v.y);
        return FastMath.hypot(x - v.x, y - v.y)
    }

    fun squareDistance(v: Point2D): Double {
        val tx = x - v.x
        val ty = y - v.y
        return tx * tx + ty * ty
    }

    fun squareDistance(x: Double, y: Double): Double {
        val tx = this.x - x
        val ty = this.y - y
        return tx * tx + ty * ty
    }

    fun squareLength(): Double {
        return x * x + y * y
    }

    fun reverse(): Point2D {
        return Point2D(-x, -y)
    }

    fun normalize(): Point2D {
        val length = this.length()
        return if (length == 0.0) {
            Point2D(0.0, 0.0)
        } else {
            Point2D(x / length, y / length)
        }
    }

    fun length(length: Double): Point2D {
        val currentLength = this.length()
        return if (currentLength == 0.0) {
            this
        } else {
            this.mul(length / currentLength)
        }
    }

    fun leftPerpendicular(): Point2D {
        return Point2D(y, -x)
    }

    fun rightPerpendicular(): Point2D {
        return Point2D(-y, x)
    }

    fun dotProduct(vector: Point2D): Double {
        return x * vector.x + y * vector.y
    }

    fun angle(): Float {
        //return Math.atan2(y, x);
        return FastMath.atan2(y.toFloat(), x.toFloat())
    }

    fun nearlyEqual(potentialIntersectionPoint: Point2D, epsilon: Double): Boolean {
        return Math.abs(x - potentialIntersectionPoint.x) < epsilon && Math.abs(y - potentialIntersectionPoint.y) < epsilon
    }

    fun rotate(angle: Point2D): Point2D {
        val newX = angle.x * x - angle.y * y
        val newY = angle.y * x + angle.x * y
        return Point2D(newX, newY)
    }

    fun rotateBack(angle: Point2D): Point2D {
        val newX = angle.x * x + angle.y * y
        val newY = angle.x * y - angle.y * x
        return Point2D(newX, newY)
    }

    operator fun div(f: Double): Point2D {
        return Point2D(x / f, y / f)
    }


    fun add(point: Point2D): Point2D {
        return add(point.x, point.y)
    }

    fun rotate(angle: Double): Point2D {

        val x1 = (this.x * Math.cos(angle) - this.y * Math.sin(angle)).toFloat()

        val y1 = (this.x * Math.sin(angle) + this.y * Math.cos(angle)).toFloat()

        return Point2D(x1.toDouble(), y1.toDouble())
    }

    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeDouble(stream, x)
        StreamUtil.writeDouble(stream, y)
    }

    fun toFloat(): Vec2Float {
        return Vec2Float(x.toFloat(), y.toFloat())
    }

    fun dirTo(pos: Point2D): Direction {
        return when {
            pos.x - 1 == x -> Direction.RIGHT
            pos.x + 1 == x -> Direction.LEFT
            pos.y + 1 == y -> Direction.DOWN
            pos.y - 1 == y -> Direction.UP
            else -> {
                MainKt.myDebugLog("unable to find dir for $this $pos")
                Direction.UP
            }
        }
    }

    fun applyDir(direction: Direction): Point2D {
        return this.add(
            when (direction) {
                Direction.LEFT -> LEFT
                Direction.UP -> UP
                Direction.RIGHT -> RIGHT
                Direction.DOWN -> DOWN
            }
        )
    }

    companion object {

        fun angle(x: Double, y: Double): Float {
            return FastMath.atan2(y.toFloat(), x.toFloat())
        }


        fun getDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            return Math.sqrt(dx * dx + dy * dy)
        }

        fun vector(fromX: Double, fromY: Double, toX: Double, toY: Double): Point2D {
            return Point2D(toX - fromX, toY - fromY)
        }

        fun readFrom(stream: java.io.InputStream): Point2D {
            val result = Point2D()
            result.x = StreamUtil.readDouble(stream)
            result.y = StreamUtil.readDouble(stream)
            return result
        }

        val UP = Point2D(0, 1)
        val RIGHT = Point2D(1, 0)
        val DOWN = Point2D(0, -1)
        val LEFT = Point2D(-1, 0)
    }
}
