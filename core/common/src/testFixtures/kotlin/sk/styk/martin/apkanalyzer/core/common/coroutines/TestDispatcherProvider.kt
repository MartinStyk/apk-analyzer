package sk.styk.martin.apkanalyzer.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TestDispatcherProvider(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher(),
    DispatcherProvider {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }

    override fun main(): CoroutineDispatcher = dispatcher

    override fun default(): CoroutineDispatcher = dispatcher

    override fun io(): CoroutineDispatcher = dispatcher

    override fun unconfined(): CoroutineDispatcher = dispatcher
}
