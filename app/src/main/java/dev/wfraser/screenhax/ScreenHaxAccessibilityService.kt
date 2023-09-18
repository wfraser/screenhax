package dev.wfraser.screenhax

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.GuardedBy
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class ScreenHaxAccessibilityService : AccessibilityService() {
    private val tag = "ScreenHaxAccessibilityService"
    private val eventInterval = Duration.ofSeconds(30)

    private val lock = ReentrantLock()
    private val cv = lock.newCondition()

    @GuardedBy("lock")
    private var enabled = true

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
        setEnabled(false)
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        Log.i(tag, "onServiceConnected")
        toast("ScreenHax enabled")
        startInjector()
        super.onServiceConnected()
    }

    fun setEnabled(enabled: Boolean) {
        lock.withLock {
            this.enabled = enabled
            cv.signalAll()
        }
    }

    fun toggleEnabled() = lock.withLock {
        setEnabled(!enabled)
        enabled
    }

    private fun toast(message: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra("toastMessage", message)
        })
    }

    /**
     * Sleep for [eventInterval] or until the value of [enabled] changes.
     * @return the current value of [enabled]
     */
    private fun sleep() = lock.withLock {
        cv.awaitNanos(eventInterval.toNanos())
        enabled
    }

    /**
     * Start a thread to inject touch events every [eventInterval] while [enabled] is true.
     */
    private fun startInjector() {
        Log.i(tag, "starting injector")
        setEnabled(true)
        thread(name = "touchInjector") {
            do touch() while (sleep())
            Log.i(tag, "injector stopped")
        }
    }

    private fun touch() {
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
}
