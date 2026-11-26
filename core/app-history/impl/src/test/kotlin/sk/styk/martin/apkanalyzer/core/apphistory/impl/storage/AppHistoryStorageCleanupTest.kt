package sk.styk.martin.apkanalyzer.core.apphistory.impl.storage

import android.content.Context
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppHistoryStorageCleanupTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val cleanup = AppHistoryStorageCleanup(context)

    @After
    fun tearDown() {
        context.deleteDatabase("unrelated.db")
        context.deleteDatabase("app_history_v2.db")
        context.deleteDatabase("app_history_v2_restore_staging.db")
    }

    @Test
    fun `Given legacy and compressed history When cleaning up Then only uncompressed history is deleted`() {
        val legacyNames = listOf("app_history.db", "app_history_restore_staging.db")
            .flatMap { listOf(it, "$it-wal", "$it-shm", "$it-journal") }
        val retainedNames = listOf("app_history_v2.db", "app_history_v2_restore_staging.db", "unrelated.db")
            .flatMap { listOf(it, "$it-wal", "$it-shm", "$it-journal") }
        context.getDatabasePath("unrelated.db").parentFile!!.mkdirs()
        (legacyNames + retainedNames).forEach { context.getDatabasePath(it).writeText("existing data") }

        cleanup.deleteUncompressedDatabases()

        legacyNames.map { context.getDatabasePath(it).exists() }.toSet() shouldBe setOf(false)
        retainedNames.map { context.getDatabasePath(it).readText() }.toSet() shouldBe setOf("existing data")
    }

    @Test
    fun `Given no history files When cleaning up twice Then no history database is created`() {
        cleanup.deleteUncompressedDatabases()

        cleanup.deleteUncompressedDatabases()

        context.getDatabasePath("app_history_v2.db").exists() shouldBe false
    }
}
