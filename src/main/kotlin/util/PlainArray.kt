package util

import model.Point2D
import kotlin.math.roundToInt

class PlainArray<T> internal constructor(val cellsWidth: Int, val cellsHeight: Int, init: (Int) -> T) {

    val array: MutableList<T>

    init {
        array = MutableList(this.cellsWidth * cellsHeight, init)
    }

    internal operator fun get(x: Int, y: Int): T? {
        return if (!inBounds(x, y)) {
            null
        } else {
            array[y * cellsWidth + x]
        }
    }

    inline fun getFast(x: Int, y: Int): T {
        return array[y * cellsWidth + x]
    }

    internal fun add(x: Int, y: Int, `val`: T) {
        if (!inBounds(x, y)) {
            return
        }
        array[y * cellsWidth + x] = `val`
    }

    internal operator fun set(x: Int, y: Int, `val`: T) {
        if (!inBounds(x, y)) {
            return
        }
        array[y * cellsWidth + x] = `val`
    }

    internal fun setFast(x: Int, y: Int, `val`: T) {
        array[y * cellsWidth + x] = `val`
    }

    fun inBounds(x: Int, y: Int): Boolean {
        return !(x < 0 || x >= cellsWidth || y < 0 || y >= cellsHeight)
    }

    fun fori(block: (x: Int, y: Int, v: T) -> Unit) {
        for (y in 0 until cellsHeight) {
            for (x in 0 until cellsWidth) {
                block(x, y, getFast(x, y))
            }
        }
    }

    fun set(pos: Point2D, value: T) {
        set(pos.x.roundToInt(), pos.y.roundToInt(), value)
    }

    fun get(pos: Point2D): T? {
        return get(pos.x.toInt(), pos.y.toInt())
    }

    fun getFastNoRound(pos: Point2D): T {
        return getFast(pos.x.toInt(), pos.y.toInt())
    }

    fun setFastNoRound(pos: Point2D, value: T) {
        setFast(pos.intX, pos.intY, value)
    }
}
