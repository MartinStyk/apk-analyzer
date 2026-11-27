package sk.styk.martin.apkanalyzer.core.designsystem.phone.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.BottomSheet

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (entry.metadata[BottomSheetKey] != true) return null
        return BottomSheetScene(
            key = entry.contentKey,
            entry = entry,
            previousEntries = entries.dropLast(1),
            onBack = onBack,
        )
    }

    companion object {
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}

private data object BottomSheetKey : NavMetadataKey<Boolean>

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val overlaidEntries: List<NavEntry<T>> = previousEntries

    override val content: @Composable () -> Unit = {
        var isDismissed by remember { mutableStateOf(false) }
        BottomSheet(
            onDismiss = {
                if (!isDismissed) {
                    isDismissed = true
                    onBack()
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            entry.Content()
        }
    }

    override fun equals(other: Any?): Boolean = other is BottomSheetScene<*> &&
        key == other.key &&
        entry == other.entry &&
        previousEntries == other.previousEntries

    override fun hashCode(): Int = (key.hashCode() * 31 + entry.hashCode()) * 31 + previousEntries.hashCode()
}
