package dev.wfraser.screenhax

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.MainThread

class MainActivity : Activity() {
    override fun onResume() {
        super.onResume()

        val msg = intent.extras?.getString("toastMessage")
        if (!msg.isNullOrEmpty()) {
            // We were launched by the service to send a toast.
            toast(msg)
        } else {
            // We were launched manually; toggle the service if it's instantiated.
            val svc = ScreenHaxAccessibilityService.instance
            if (svc != null) {
                if (svc.enabled) {
                    svc.enabled = false
                    toast("ScreenHax disabled")
                } else {
                    svc.enabled = true
                    toast("ScreenHax enabled")
                }
            } else {
                toast("ScreenHax needs to be enabled in Accessibility settings")
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        finish()
    }

    @MainThread
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
