package sk.styk.martin.apkanalyzer.core.common.logger

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

object Logger {
    fun init(logToConsole: Boolean) {
        if (logToConsole) Timber.plant(Timber.DebugTree())
        Timber.plant(FirebaseTree())
    }

    fun v(tag: String, message: String) = Timber.tag(tag).v(message)

    fun d(tag: String, message: String) = Timber.tag(tag).d(message)

    fun i(tag: String, message: String) = Timber.tag(tag).i(message)

    fun w(tag: String, message: String) = Timber.tag(tag).w(message)

    fun w(
        tag: String,
        t: Throwable,
        message: String,
    ) = Timber.tag(tag).w(t, message)

    fun e(tag: String, message: String) = Timber.tag(tag).e(message)

    fun e(
        tag: String,
        t: Throwable,
        message: String,
    ) = Timber.tag(tag).e(t, message)

    fun log(
        tag: String,
        event: LogEvent,
        throwable: Throwable? = null,
    ) {
        val priority = when (event) {
            is LogEvent.ScreenOpen -> Log.INFO

            is LogEvent.Operation -> when (event.state) {
                LogEvent.Operation.State.Started -> Log.DEBUG
                LogEvent.Operation.State.Succeeded -> if (event.stage == null) Log.INFO else Log.DEBUG
                LogEvent.Operation.State.Degraded -> Log.WARN
                LogEvent.Operation.State.Failed -> Log.ERROR
            }
        }
        val message = when (event) {
            is LogEvent.ScreenOpen -> "event=screen_opened key=${event.key}"

            is LogEvent.Operation -> buildString {
                append("operation=").append(event.name)
                append(" request=").append(event.request.id)
                event.stage?.let { append(" stage=").append(it) }
                append(" event=").append(event.state.value)
                event.context?.let { append(' ').append(it) }
            }
        }
        Timber.tag(tag).log(priority, throwable, message)
    }

    private class FirebaseTree : Timber.DebugTree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            FirebaseCrashlytics.getInstance().log("${priority.toPriorityChar()}/${tag.orEmpty()}: $message")
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
