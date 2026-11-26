package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class PackageChangesObserverFake : PackageChangesObserver {
    private val events = MutableSharedFlow<PackageChangeEvent>(extraBufferCapacity = 1)
    private val beforeNotifying = mutableListOf<(PackageChangeEvent) -> Unit>()

    val subscriberCount: Int
        get() = events.subscriptionCount.value

    override fun observe(): Flow<PackageChangeEvent> = events

    override fun runBeforeNotifying(action: (PackageChangeEvent) -> Unit) {
        beforeNotifying += action
    }

    suspend fun emit(event: PackageChangeEvent) {
        beforeNotifying.forEach { it(event) }
        events.emit(event)
    }
}
