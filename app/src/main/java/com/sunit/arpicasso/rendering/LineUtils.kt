/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sunit.arpicasso.rendering

import android.opengl.Matrix
import com.sunit.arpicasso.AppSettings
import javax.vecmath.Vector2f
import javax.vecmath.Vector3f

object LineUtils {
    /**
     * @param value
     * @param inputMin
     * @param inputMax
     * @param outputMin
     * @param outputMax
     * @param clamp
     * @return
     */
    @JvmStatic
    fun map(
        value: Float,
        inputMin: Float,
        inputMax: Float,
        outputMin: Float,
        outputMax: Float,
        clamp: Boolean
    ): Float {
        var outVal =
            (value - inputMin) / (inputMax - inputMin) * (outputMax - outputMin) + outputMin
        if (clamp) {
            if (outputMax < outputMin) {
                if (outVal < outputMax) outVal = outputMax else if (outVal > outputMin) outVal =
                    outputMin
            } else {
                if (outVal > outputMax) outVal = outputMax else if (outVal < outputMin) outVal =
                    outputMin
            }
        }
        return outVal
    }

    /**
     * @param start
     * @param stop
     * @param amt
     * @return
     */
    fun lerp(start: Float, stop: Float, amt: Float): Float {
        return start + (stop - start) * amt
    }

    /**
     * @param touchPoint
     * @param screenWidth
     * @param screenHeight
     * @param projectionMatrix
     * @param viewMatrix
     * @return
     */
    @JvmStatic
    fun GetWorldCoords(
        touchPoint: Vector2f,
        screenWidth: Float,
        screenHeight: Float,
        projectionMatrix: FloatArray?,
        viewMatrix: FloatArray?
    ): Vector3f {
        val touchRay =
            projectRay(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix)
        touchRay.direction.scale(AppSettings.strokeDrawDistance)
        touchRay.origin.add(touchRay.direction)
        return touchRay.origin
    }

    /**
     * @param point
     * @param viewportSize
     * @param viewProjMtx
     * @return
     */
    fun screenPointToRay(point: Vector2f, viewportSize: Vector2f, viewProjMtx: FloatArray?): Ray {
        point.y = viewportSize.y - point.y
        val x = point.x * 2.0f / viewportSize.x - 1.0f
        val y = point.y * 2.0f / viewportSize.y - 1.0f
        val farScreenPoint = floatArrayOf(x, y, 1.0f, 1.0f)
        val nearScreenPoint = floatArrayOf(x, y, -1.0f, 1.0f)
        val nearPlanePoint = FloatArray(4)
        val farPlanePoint = FloatArray(4)
        val invertedProjectionMatrix = FloatArray(16)
        Matrix.setIdentityM(invertedProjectionMatrix, 0)
        Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0)
        Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0)
        Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0)
        val direction = Vector3f(
            farPlanePoint[0] / farPlanePoint[3],
            farPlanePoint[1] / farPlanePoint[3],
            farPlanePoint[2] / farPlanePoint[3]
        )
        val origin = Vector3f(
            Vector3f(
                nearPlanePoint[0] / nearPlanePoint[3],
                nearPlanePoint[1] / nearPlanePoint[3],
                nearPlanePoint[2] / nearPlanePoint[3]
            )
        )
        direction.sub(origin)
        direction.normalize()
        return Ray(origin, direction)
    }

    /**
     * @param touchPoint
     * @param screenWidth
     * @param screenHeight
     * @param projectionMatrix
     * @param viewMatrix
     * @return
     */
    fun projectRay(
        touchPoint: Vector2f,
        screenWidth: Float,
        screenHeight: Float,
        projectionMatrix: FloatArray?,
        viewMatrix: FloatArray?
    ): Ray {
        val viewProjMtx = FloatArray(16)
        Matrix.multiplyMM(viewProjMtx, 0, projectionMatrix, 0, viewMatrix, 0)
        return screenPointToRay(touchPoint, Vector2f(screenWidth, screenHeight), viewProjMtx)
    }

    /**
     * @param newPoint
     * @param lastPoint
     * @return
     */
    @JvmStatic
    fun distanceCheck(newPoint: Vector3f?, lastPoint: Vector3f?): Boolean {
        val temp = Vector3f()
        temp.sub(newPoint, lastPoint)
        return temp.length() > AppSettings.minDistance
    }
}