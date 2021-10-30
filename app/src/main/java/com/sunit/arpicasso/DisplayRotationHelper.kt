package com.sunit.arpicasso

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.google.ar.core.Session

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus they require listening to the android display
 * events.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
class DisplayRotationHelper @RequiresApi(api = Build.VERSION_CODES.M) constructor(private val context: Context) :
    DisplayListener {
    private var viewportChanged = false
    private var viewportWidth = 0
    private var viewportHeight = 0
    private val display: Display

    /** Registers the display listener. Should be called from [Activity.onResume].  */
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun onResume() {
        context.getSystemService(DisplayManager::class.java).registerDisplayListener(this, null)
    }

    /** Unregisters the display listener. Should be called from [Activity.onPause].  */
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun onPause() {
        context.getSystemService(DisplayManager::class.java).unregisterDisplayListener(this)
    }

    /**
     * Records a change in surface dimensions. This will be later used by [ ][.updateSessionIfNeeded]. Should be called from [ ].
     *
     * @param width the updated width of the surface.
     * @param height the updated height of the surface.
     */
    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    /**
     * Updates the session display geometry if a change was posted either by [ ][.onSurfaceChanged] call or by [.onDisplayChanged] system callback. This
     * function should be called explicitly before each call to [Session.update]. This
     * function will also clear the 'pending update' (viewportChanged) flag.
     *
     * @param session the [Session] object to update if display geometry changed.
     */
    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            val displayRotation = display.rotation
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    /**
     * Returns the current rotation state of android display. Same as [Display.getRotation].
     */
    val rotation: Int
        get() = display.rotation

    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}
    override fun onDisplayChanged(displayId: Int) {
        viewportChanged = true
    }

    /**
     * Constructs the DisplayRotationHelper but does not register the listener yet.
     *
     * @param context the Android [Context].
     */
    init {
        display = context.getSystemService(WindowManager::class.java).defaultDisplay
    }
}