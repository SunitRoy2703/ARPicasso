package com.sunit.arpicasso

import javax.vecmath.Vector3f

/**
 * BiquadFilter is a object for easily lowpass filtering incomming values.
 */
class BiquadFilter internal constructor(Fc: Double) {
    private val `val` = Vector3f()
    private val inst = arrayOfNulls<BiquadFilterInstance>(3)
    fun update(`in`: Vector3f): Vector3f {
        `val`.x = inst[0]!!.process(`in`.x.toDouble()).toFloat()
        `val`.y = inst[1]!!.process(`in`.y.toDouble()).toFloat()
        `val`.z = inst[2]!!.process(`in`.z.toDouble()).toFloat()
        return `val`
    }

    private inner class BiquadFilterInstance internal constructor(fc: Double) {
        var a0 = 0.0
        var a1 = 0.0
        var a2 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var Fc = 0.5
        var Q = 0.707
        var peakGain = 0.0
        var z1 = 0.0
        var z2 = 0.0
        fun process(`in`: Double): Double {
            val out = `in` * a0 + z1
            z1 = `in` * a1 + z2 - b1 * out
            z2 = `in` * a2 - b2 * out
            return out
        }

        fun calcBiquad() {
            val norm: Double
            val K = Math.tan(Math.PI * Fc)
            norm = 1 / (1 + K / Q + K * K)
            a0 = K * K * norm
            a1 = 2 * a0
            a2 = a0
            b1 = 2 * (K * K - 1) * norm
            b2 = (1 - K / Q + K * K) * norm
        }

        init {
            Fc = fc
            calcBiquad()
        }
    }

    init {
        for (i in 0..2) {
            inst[i] = BiquadFilterInstance(Fc)
        }
    }
}