package com.sunit.arpicasso

import javax.vecmath.Vector3f

object AppSettings {

    var color = Vector3f(1f, 1f, 1f)

    const val strokeDrawDistance = 0.125f

    const val minDistance = 0.000625f

    const val nearClip = 0.001f

    const val farClip = 100.0f

    @JvmName("setColor1")
    private fun setColor(vector3f: Vector3f){
        color = vector3f
    }

}