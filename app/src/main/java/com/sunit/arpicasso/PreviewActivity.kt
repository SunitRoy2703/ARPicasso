package com.sunit.arpicasso

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PreviewActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    var uriString: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        uriString = intent.getStringExtra("image_uri")
        preview = findViewById(R.id.preview)
        preview.setImageURI(Uri.parse(uriString))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //if (mPlaybackView == null || !mPlaybackView.isOpen())
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun closeActivity(view: View?) {
        val file = File(uriString)
        val del = file.delete()
        this@PreviewActivity.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)
            )
        )
        onBackPressed()
    }

    fun saveImage(view: View?) {
        Toast.makeText(this, R.string.Picture_Saved, Toast.LENGTH_SHORT).show()
        onBackPressed()
    }

    fun shareImage(view: View?) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString))
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.ShareMessage))
        shareIntent.type = getString(R.string.image_jpeg)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.ShareTo)))
    }
}