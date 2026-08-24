package sk.styk.martin.apkanalyzer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_LIST_ITEM_TAG = "app_list_item_row"
private const val UI_TIMEOUT_MS = 10_000L

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = "sk.styk.martin.apkanalyzer",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun appsToAppDetail() = baselineProfileRule.collect(
        packageName = "sk.styk.martin.apkanalyzer",
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.res(APP_LIST_ITEM_TAG)), UI_TIMEOUT_MS)
        device.findObject(By.res(APP_LIST_ITEM_TAG)).click()
        device.wait(Until.gone(By.res(APP_LIST_ITEM_TAG)), UI_TIMEOUT_MS)
    }
}
