package dev.wfraser.screenhax

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.time.Duration
import kotlin.concurrent.thread

class ScreenHaxAccessibilityService : AccessibilityService() {
    val tag = "ScreenHaxAccessibilityService"
    val eventInterval = Duration.ofSeconds(30)

    @Volatile
    var enabled = true

    companion object {
        @Volatile
        var instance: ScreenHaxAccessibilityService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We're not manifested to actually receive any though.
        Log.i(tag, "onAccessibilityEvent: $event")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(tag, "onUnbind")
        toast("ScreenHax disabled")
        enabled = false
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        Log.i(tag, "onServiceConnected")
        toast("ScreenHax enabled")
        startInjector()
        super.onServiceConnected()
    }

    private fun toast(message: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra("toastMessage", message)
        })
    }

    private fun startInjector() {
        Log.i(tag, "starting injector")
        enabled = true
        thread(name = "touchInjector") {
            for (i in generateSequence { Thread.sleep(eventInterval.toMillis()) }) {
                if (!enabled) {
                    break
                }

                val desc = GestureDescription.Builder().apply {
                    addStroke(
                        GestureDescription.StrokeDescription(
                            Path().apply { moveTo(0F, 0F) },
                            0,
                            1,
                        )
                    )
                }.build()

                val cb = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.i(tag, "gesture completed")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.i(tag, "gesture cancelled")
                    }
                }

                val ok = dispatchGesture(desc, cb, null)
                Log.i(tag, "gesture dispatched? $ok")
            }
            Log.i(tag, "injector stopped")
        }
    }
}