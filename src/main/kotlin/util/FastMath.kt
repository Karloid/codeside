package util

object FastMath {
    private val TWO_POW_450 = java.lang.Double.longBitsToDouble(0x5C10000000000000L)
    private val TWO_POW_N450 = java.lang.Double.longBitsToDouble(0x23D0000000000000L)
    private val TWO_POW_750 = java.lang.Double.longBitsToDouble(0x6ED0000000000000L)
    private val TWO_POW_N750 = java.lang.Double.longBitsToDouble(0x1110000000000000L)
    fun hypot(x: Double, y: Double): Double {
        var x = x
        var y = y
        x = Math.abs(x)
        y = Math.abs(y)
        if (y < x) {
            val a = x
            x = y
            y = a
        } else if (y < x) { // Testing if we have some NaN.
            return if (x == Double.POSITIVE_INFINITY || y == Double.POSITIVE_INFINITY) {
                Double.POSITIVE_INFINITY
            } else {
                Double.NaN
            }
        }
        return if (y - x == y) { // x too small to substract from y
            y
        } else {
            val factor: Double
            if (x > TWO_POW_450) { // 2^450 < x < y
                x *= TWO_POW_N750
                y *= TWO_POW_N750
                factor = TWO_POW_750
            } else if (y < TWO_POW_N450) { // x < y < 2^-450
                x *= TWO_POW_750
                y *= TWO_POW_750
                factor = TWO_POW_N750
            } else {
                factor = 1.0
            }
            factor * Math.sqrt(x * x + y * y)
        }
    }

    private const val SIZE = 1024
    private const val STRETCH = Math.PI.toFloat()
    // Output will swing from -STRETCH to STRETCH (default: Math.PI)
// Useful to change to 1 if you would normally do "atan2(y, x) / Math.PI"
// Inverse of SIZE
    private const val EZIS = -SIZE
    private val ATAN2_TABLE_PPY = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_PPX = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_PNY = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_PNX = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_NPY = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_NPX = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_NNY = FloatArray(SIZE + 1)
    private val ATAN2_TABLE_NNX = FloatArray(SIZE + 1)
    /**
     * ATAN2
     */
    fun atan2(y: Float, x: Float): Float {
        return if (x >= 0) {
            if (y >= 0) {
                if (x >= y) ATAN2_TABLE_PPY[(SIZE * y / x + 0.5).toInt()] else ATAN2_TABLE_PPX[(SIZE * x / y + 0.5).toInt()]
            } else {
                if (x >= -y) ATAN2_TABLE_PNY[(EZIS * y / x + 0.5).toInt()] else ATAN2_TABLE_PNX[(EZIS * x / y + 0.5).toInt()]
            }
        } else {
            if (y >= 0) {
                if (-x >= y) ATAN2_TABLE_NPY[(EZIS * y / x + 0.5).toInt()] else ATAN2_TABLE_NPX[(EZIS * x / y + 0.5).toInt()]
            } else {
                if (x <= y) // (-x >= -y)
                    ATAN2_TABLE_NNY[(SIZE * y / x + 0.5).toInt()] else ATAN2_TABLE_NNX[(SIZE * x / y + 0.5).toInt()]
            }
        }
    }

    init {
        for (i in 0..SIZE) {
            val f = i.toFloat() / SIZE
            ATAN2_TABLE_PPY[i] =
                (StrictMath.atan(f.toDouble()) * STRETCH / StrictMath.PI).toFloat()
            ATAN2_TABLE_PPX[i] = STRETCH * 0.5f - ATAN2_TABLE_PPY[i]
            ATAN2_TABLE_PNY[i] = -ATAN2_TABLE_PPY[i]
            ATAN2_TABLE_PNX[i] = ATAN2_TABLE_PPY[i] - STRETCH * 0.5f
            ATAN2_TABLE_NPY[i] = STRETCH - ATAN2_TABLE_PPY[i]
            ATAN2_TABLE_NPX[i] = ATAN2_TABLE_PPY[i] + STRETCH * 0.5f
            ATAN2_TABLE_NNY[i] = ATAN2_TABLE_PPY[i] - STRETCH
            ATAN2_TABLE_NNX[i] = -STRETCH * 0.5f - ATAN2_TABLE_PPY[i]
        }
    }
}