package sk.styk.martin.apkanalyzer.core.common.logger

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

object Logger {
    fun init(logToConsole: Boolean) {
        val trees =
            buildList {
                if (logToConsole) add(Timber.DebugTree())
                add(FirebaseTree())
            }
        Timber.plant(*trees.toTypedArray())
    }

    fun v(tag: String, message: String) = Timber.tag(tag).v(message)

    fun d(tag: String, message: String) = Timber.tag(tag).d(message)

    fun i(tag: String, message: String) = Timber.tag(tag).i(message)

    fun w(tag: String, message: String) = Timber.tag(tag).w(message)

    fun w(tag: String, t: Throwable, message: String) = Timber.tag(tag).w(t, message)

    fun e(tag: String, message: String) = Timber.tag(tag).e(message)

    fun e(tag: String, t: Throwable, message: String) = Timber.tag(tag).e(t, message)

    private class FirebaseTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            FirebaseCrashlytics.getInstance().log("${priority.toPriorityChar()}/$tag: $message")
            if (priority >= Log.WARN && t != null) {
                FirebaseCrashlytics.getInstance().recordException(t)
            }
        }

        private fun Int.toPriorityChar() = when (this) {
            Log.ASSERT, Log.ERROR -> 'E'
            Log.WARN -> 'W'
            Log.INFO -> 'I'
            Log.DEBUG -> 'D'
            else -> 'V'
        }
    }
}
