package dev.wfraser.screenhax

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread

class MainActivity : Activity() {
    private val tag = "MainActivity"

    override fun onResume() {
        super.onResume()

        // Were we launched by the service to send a toast?
        var msg = intent.extras?.getString("toastMessage")
        if (msg.isNullOrEmpty()) {
            // We were launched manually; toggle the service if it's instantiated.
            val svc = ScreenHaxAccessibilityService.instance
            if (svc != null) {
                Log.i(tag, "toggling service")
                msg = if (svc.toggleEnabled()) {
                    "ScreenHax enabled"
                } else {
                    "ScreenHax disabled"
                }
            } else {
                Log.i(tag, "no instance of service yet")
                msg = "ScreenHax needs to be enabled in Accessibility settings"
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        } else {
            Log.i(tag, "started with toast param \"$msg\"")
        }

        toast(msg)

        // Activity which uses @android:style/Theme.NoDisplay is required to call finish before
        // returning from onResume.
        finish()
    }

    @MainThread
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
