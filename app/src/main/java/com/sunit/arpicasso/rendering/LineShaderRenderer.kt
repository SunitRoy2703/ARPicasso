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

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.sunit.arpicasso.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.vecmath.Vector3f

/**
 * Renders a point cloud.
 */
class LineShaderRenderer {
    private val mModelMatrix = FloatArray(16)
    private val mModelViewMatrix = FloatArray(16)
    private val mModelViewProjectionMatrix = FloatArray(16)
    private var mPositionAttribute = 0
    private var mPreviousAttribute = 0
    private var mNextAttribute = 0
    private var mSideAttribute = 0
    private var mWidthAttribte = 0
    private var mCountersAttribute = 0
    private var mProjectionUniform = 0
    private var mModelViewUniform = 0
    private var mResolutionUniform = 0
    private var mLineWidthUniform = 0
    private var mColorUniform = 0
    private var mOpacityUniform = 0
    private var mNearUniform = 0
    private var mFarUniform = 0
    private var mSizeAttenuationUniform = 0
    private var mDrawModeUniform = 0
    private var mNearCutoffUniform = 0
    private var mFarCutoffUniform = 0
    private var mDrawMode = false
    private var mVisibility = 0
    private var mAlphaTest = 0
    private lateinit var mPositions: FloatArray
    private lateinit var mCounters: FloatArray
    private lateinit var mNext: FloatArray
    private var mSide: FloatArray? = null
    private lateinit var mWidth: FloatArray
    private lateinit var mPrevious: FloatArray
    private var mPositionAddress = 0
    private var mPreviousAddress = 0
    private var mNextAddress = 0
    private var mSideAddress = 0
    private var mWidthAddress = 0
    private var mCounterAddress = 0
    private var mNumPoints = 0
    private var mNumBytes = 0
    private var mVbo = 0
    private var mVboSize = 0
    private var mProgramName = 0
    private var lineWidth = 0f
    private var mColor: Vector3f? = null
    @JvmField
    var bNeedsUpdate = AtomicBoolean()
    private var mLineDepthScaleUniform = 0
    private var mLineDepthScale = 10.0f
    @JvmField
    var mDrawDistance = 0f

    /**
     * Allocates and initializes OpenGL resources needed by the Line renderer.  Must be
     * called on the OpenGL thread, typically in
     * [GLSurfaceView.Renderer.onSurfaceCreated].
     *
     * @param context Needed to access shader source.
     */
    fun createOnGlThread(context: Context?) {
        ShaderUtil.checkGLError(TAG, "before create")
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        mVbo = buffers[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo)
        mVboSize = 0
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "buffer alloc")
        /**
         *
         * The LineShaderRenderer uses an ES2 pipeline.  It uses the line_vert.glsl and
         * line_frag.glsl shader to render a volumetric line.  It uses several techniques detailed in
         * the following resources:
         *
         * Drawing Lines is Hard by Matt DesLauriers
         * https://mattdesl.svbtle.com/drawing-lines-is-hard
         *
         * InkSpace an Android Experiment by Zach Lieberman
         * https://experiments.withgoogle.com/android/ink-space
         * https://github.com/ofZach/inkSpace
         *
         * THREEJS.MeshLine by Jaume Sanchez
         * https://github.com/spite/THREE.MeshLine/blob/master/src/THREE.MeshLine.js
         *
         *
         * The Renderer batches all of the geometry into a single VBO.  This allows us to have a single
         * draw call to render the geometry.  We also optimize the application to only re-upload the
         * geometry data when a new stroke or new points are added to the drawing. The renderer uses
         * a technique detailed in the following link to create degenerate faces between the strokes
         * to disconnect them from one another.
         * https://developer.apple.com/library/content/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/TechniquesforWorkingwithVertexData/TechniquesforWorkingwithVertexData.html
         *
         */
        val vertexShader = ShaderUtil.loadGLShader(
            TAG, context!!,
            GLES20.GL_VERTEX_SHADER, R.raw.line_vert
        )
        val fragmentShader = ShaderUtil.loadGLShader(
            TAG, context!!,
            GLES20.GL_FRAGMENT_SHADER, R.raw.line_frag
        )
        mProgramName = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgramName, vertexShader)
        GLES20.glAttachShader(mProgramName, fragmentShader)
        GLES20.glLinkProgram(mProgramName)
        GLES20.glUseProgram(mProgramName)
        ShaderUtil.checkGLError(TAG, "program")
        mPositionAttribute = GLES20.glGetAttribLocation(mProgramName, "position")
        mPreviousAttribute = GLES20.glGetAttribLocation(mProgramName, "previous")
        mNextAttribute = GLES20.glGetAttribLocation(mProgramName, "next")
        mSideAttribute = GLES20.glGetAttribLocation(mProgramName, "side")
        mWidthAttribte = GLES20.glGetAttribLocation(mProgramName, "width")
        mCountersAttribute = GLES20.glGetAttribLocation(mProgramName, "counters")
        mProjectionUniform = GLES20.glGetUniformLocation(mProgramName, "projectionMatrix")
        mModelViewUniform = GLES20.glGetUniformLocation(mProgramName, "modelViewMatrix")
        mResolutionUniform = GLES20.glGetUniformLocation(mProgramName, "resolution")
        mLineWidthUniform = GLES20.glGetUniformLocation(mProgramName, "lineWidth")
        mColorUniform = GLES20.glGetUniformLocation(mProgramName, "color")
        mOpacityUniform = GLES20.glGetUniformLocation(mProgramName, "opacity")
        mNearUniform = GLES20.glGetUniformLocation(mProgramName, "near")
        mFarUniform = GLES20.glGetUniformLocation(mProgramName, "far")
        mSizeAttenuationUniform = GLES20.glGetUniformLocation(mProgramName, "sizeAttenuation")
        mVisibility = GLES20.glGetUniformLocation(mProgramName, "visibility")
        mAlphaTest = GLES20.glGetUniformLocation(mProgramName, "alphaTest")
        mDrawModeUniform = GLES20.glGetUniformLocation(mProgramName, "drawMode")
        mNearCutoffUniform = GLES20.glGetUniformLocation(mProgramName, "nearCutOff")
        mFarCutoffUniform = GLES20.glGetUniformLocation(mProgramName, "farCutOff")
        mLineDepthScaleUniform = GLES20.glGetUniformLocation(mProgramName, "lineDepthScale")
        ShaderUtil.checkGLError(TAG, "program  params")
        Matrix.setIdentityM(mModelMatrix, 0)
        mColor = Vector3f(1f, 1f, 1f)
        lineWidth = 0.5f
    }

    /**
     * Sets the LineWidth of the Line.
     * Requires bNeedsUpdate.set(true) to take effect
     * @param width
     */
    fun setLineWidth(width: Float) {
        lineWidth = width
    }

    /**
     * Enables or Disables the Debug View in the Fragment Shader.  Debug View highlights the strokes
     * at the same depth as the user.  It allows the user to position new drawings to intersect or
     * bypass the existing strokes
     * @param drawDebugMode
     */
    fun setDrawDebug(drawDebugMode: Boolean) {
        mDrawMode = drawDebugMode
    }

    /**
     * This sets the color of the line by setting the color uniform in the shader.
     * @param color a Vector3f representing R, G, B for the X, Y, Z values
     */
    fun setColor(color: Vector3f?) {
        mColor = Vector3f(color)
    }

    /**
     * This sets a feature in the vertex shader to scale the line width based on the distance away
     * from the current view.
     * @param distanceScale
     */
    fun setDistanceScale(distanceScale: Float) {
        mLineDepthScale = distanceScale
    }

    /**
     * This updates the geometry data to be rendered. It ensures the capacity of the float arrays
     * and then calls addLine to generate the geometry.
     * @param strokes a ArrayList of ArrayLists of Vector3fs in world space.  The outer ArrayList
     * contains the strokes, while the inner ArrayList contains the Vertex of each Line
     */
    fun updateStrokes(strokes: ArrayList<ArrayList<Vector3f>>) {
        mNumPoints = 0
        for (l in strokes) {
            mNumPoints += l.size * 2 + 2
        }
        ensureCapacity(mNumPoints)
        var offset = 0
        for (l in strokes) {
            offset = addLine(l, offset)
        }
        mNumBytes = offset
    }

    /**
     * This ensures the capacity of the float arrays that hold the information bound to the Vertex
     * Attributes needed to render the line with the Vertex and Fragment shader.
     * @param numPoints
     */
    private fun ensureCapacity(numPoints: Int) {
        var count = 1024
        if (mSide != null) {
            count = mSide!!.size
        }
        while (count < numPoints) {
            count += 1024
        }
        if (mSide == null || mSide!!.size < count) {
            Log.i(TAG, "alloc $count")
            mPositions = FloatArray(count * 3)
            mNext = FloatArray(count * 3)
            mPrevious = FloatArray(count * 3)
            mCounters = FloatArray(count)
            mSide = FloatArray(count)
            mWidth = FloatArray(count)
        }
    }

    /**
     * AddLine takes in the 3D positions adds to the buffers to create the stroke and the degenerate
     * faces needed so the lines render properly.
     * @param line
     * @param offset
     * @return
     */
    private fun addLine(line: List<Vector3f>?, offset: Int): Int {
        if (line == null || line.size < 2) return offset
        val lineSize = line.size
        var ii = offset
        for (i in 0 until lineSize) {
            var iGood = i
            if (iGood < 0) iGood = 0
            if (iGood >= lineSize) iGood = lineSize - 1
            val i_m_1 = if (iGood - 1 < 0) iGood else iGood - 1
            val i_p_1 = if (iGood + 1 > lineSize - 1) iGood else iGood + 1
            val c = i.toFloat() / lineSize
            val current = line[iGood]
            val previous = line[i_m_1]
            val next = line[i_p_1]
            if (i == 0) {
                setMemory(ii++, current, previous, next, c, lineWidth, 1f)
            }
            setMemory(ii++, current, previous, next, c, lineWidth, 1f)
            setMemory(ii++, current, previous, next, c, lineWidth, -1f)
            if (i == lineSize - 1) {
                setMemory(ii++, current, previous, next, c, lineWidth, -1f)
            }
        }
        return ii
    }

    /**
     *
     * setMemory is a helper method used to add the stroke data to the float[] buffers
     * @param index
     * @param pos
     * @param prev
     * @param next
     * @param counter
     * @param width
     * @param side
     */
    private fun setMemory(
        index: Int,
        pos: Vector3f,
        prev: Vector3f,
        next: Vector3f,
        counter: Float,
        width: Float,
        side: Float
    ) {
        mPositions[index * 3] = pos.x
        mPositions[index * 3 + 1] = pos.y
        mPositions[index * 3 + 2] = pos.z
        mNext[index * 3] = next.x
        mNext[index * 3 + 1] = next.y
        mNext[index * 3 + 2] = next.z
        mPrevious[index * 3] = prev.x
        mPrevious[index * 3 + 1] = prev.y
        mPrevious[index * 3 + 2] = prev.z
        mCounters[index] = counter
        mSide!![index] = side
        mWidth[index] = width
    }

    /**
     * Sets the bNeedsUpdate to true.
     */
    fun clear() {
        bNeedsUpdate.set(true)
    }

    /**
     * This takes the float[] and creates FloatBuffers, Binds the VBO, and upload the Attributes to
     * correct locations with the correct offsets so the Vertex and Fragment shader can render the lines
     */
    fun upload() {
        bNeedsUpdate.set(false)
        val current = toFloatBuffer(mPositions)
        val next = toFloatBuffer(mNext)
        val previous = toFloatBuffer(mPrevious)
        val side = toFloatBuffer(mSide)
        val width = toFloatBuffer(mWidth)
        val counter = toFloatBuffer(mCounters)


//        mNumPoints = mPositions.length;
        mPositionAddress = 0
        mNextAddress = mPositionAddress + mNumBytes * 3 * BYTES_PER_FLOAT
        mPreviousAddress = mNextAddress + mNumBytes * 3 * BYTES_PER_FLOAT
        mSideAddress = mPreviousAddress + mNumBytes * 3 * BYTES_PER_FLOAT
        mWidthAddress = mSideAddress + mNumBytes * BYTES_PER_FLOAT
        mCounterAddress = mWidthAddress + mNumBytes * BYTES_PER_FLOAT
        mVboSize = mCounterAddress + mNumBytes * BYTES_PER_FLOAT
        ShaderUtil.checkGLError(TAG, "before update")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mPositionAddress, mNumBytes * 3 * BYTES_PER_FLOAT,
            current
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mNextAddress, mNumBytes * 3 * BYTES_PER_FLOAT,
            next
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mPreviousAddress, mNumBytes * 3 * BYTES_PER_FLOAT,
            previous
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mSideAddress, mNumBytes * BYTES_PER_FLOAT,
            side
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mWidthAddress, mNumBytes * BYTES_PER_FLOAT,
            width
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, mCounterAddress, mNumBytes * BYTES_PER_FLOAT,
            counter
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "after update")
    }

    /**
     *
     * This method takes in the current CameraView Matrix and the Camera's Projection Matrix, the
     * current position and pose of the device, uses those to calculate the ModelViewMatrix and
     * ModelViewProjectionMatrix.  It binds the VBO, enables the custom attribute locations,
     * binds and uploads the shader uniforms, calls our single DrawArray call, and finally disables
     * and unbinds the shader attributes and VBO.
     *
     * @param cameraView
     * @param cameraPerspective
     * @param screenWidth
     * @param screenHeight
     * @param nearClip
     * @param farClip
     */
    fun draw(
        cameraView: FloatArray?,
        cameraPerspective: FloatArray?,
        screenWidth: Float,
        screenHeight: Float,
        nearClip: Float,
        farClip: Float
    ) {
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0)
        ShaderUtil.checkGLError(TAG, "Before draw")
        GLES20.glUseProgram(mProgramName)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo)
        GLES20.glVertexAttribPointer(
            mPositionAttribute,
            FLOATS_PER_POINT,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_POINT,
            mPositionAddress
        )
        GLES20.glVertexAttribPointer(
            mPreviousAttribute,
            FLOATS_PER_POINT,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_POINT,
            mPreviousAddress
        )
        GLES20.glVertexAttribPointer(
            mNextAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, mNextAddress
        )
        GLES20.glVertexAttribPointer(
            mSideAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mSideAddress
        )
        GLES20.glVertexAttribPointer(
            mWidthAttribte, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mWidthAddress
        )
        GLES20.glVertexAttribPointer(
            mCountersAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mCounterAddress
        )
        GLES20.glUniformMatrix4fv(
            mModelViewUniform, 1, false, mModelViewMatrix, 0
        )
        GLES20.glUniformMatrix4fv(
            mProjectionUniform, 1, false, cameraPerspective, 0
        )
        GLES20.glUniform2f(mResolutionUniform, screenWidth, screenHeight)
        GLES20.glUniform1f(mLineWidthUniform, 0.01f)
        GLES20.glUniform3f(mColorUniform, mColor!!.x, mColor!!.y, mColor!!.z)
        GLES20.glUniform1f(mOpacityUniform, 1.0f)
        GLES20.glUniform1f(mNearUniform, nearClip)
        GLES20.glUniform1f(mFarUniform, farClip)
        GLES20.glUniform1f(mSizeAttenuationUniform, 1.0f)
        GLES20.glUniform1f(mVisibility, 1.0f)
        GLES20.glUniform1f(mAlphaTest, 1.0f)
        GLES20.glUniform1f(mDrawModeUniform, if (mDrawMode) 1.0f else 0.0f)
        GLES20.glUniform1f(mNearCutoffUniform, mDrawDistance - 0.0075f)
        GLES20.glUniform1f(mFarCutoffUniform, mDrawDistance + 0.0075f)
        GLES20.glUniform1f(mLineDepthScaleUniform, mLineDepthScale)
        GLES20.glEnableVertexAttribArray(mPositionAttribute)
        GLES20.glEnableVertexAttribArray(mPreviousAttribute)
        GLES20.glEnableVertexAttribArray(mNextAttribute)
        GLES20.glEnableVertexAttribArray(mSideAttribute)
        GLES20.glEnableVertexAttribArray(mWidthAttribte)
        GLES20.glEnableVertexAttribArray(mCountersAttribute)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mNumBytes)
        GLES20.glDisableVertexAttribArray(mCountersAttribute)
        GLES20.glDisableVertexAttribArray(mWidthAttribte)
        GLES20.glDisableVertexAttribArray(mSideAttribute)
        GLES20.glDisableVertexAttribArray(mNextAttribute)
        GLES20.glDisableVertexAttribArray(mPreviousAttribute)
        GLES20.glDisableVertexAttribArray(mPositionAttribute)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "Draw")
    }

    /**
     * A helper function to allocate a FloatBuffer the size of our float[] and copy the float[] into
     * the newly created FloatBuffer.
     * @param data
     * @return
     */
    private fun toFloatBuffer(data: FloatArray?): FloatBuffer {
        val buff: FloatBuffer
        val bb = ByteBuffer.allocateDirect(data!!.size * BYTES_PER_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        buff = bb.asFloatBuffer()
        buff.put(data)
        buff.position(0)
        return buff
    }

    companion object {
        private val TAG = LineShaderRenderer::class.java.simpleName
        private const val FLOATS_PER_POINT = 3 // X,Y,Z.
        private const val BYTES_PER_FLOAT = 4
        private const val BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT
    }
}