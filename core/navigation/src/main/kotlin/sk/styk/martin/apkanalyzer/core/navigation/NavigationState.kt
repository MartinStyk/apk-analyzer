package sk.styk.martin.apkanalyzer.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

class NavigationState(val startKey: NavKey, topLevelKey: MutableState<NavKey>, val backStacks: Map<NavKey, NavBackStack<NavKey>>) {
    var topLevelKey: NavKey by topLevelKey

    val stacksInUse: List<NavKey>
        get() =
            if (topLevelKey == startKey) {
                listOf(startKey)
            } else {
                listOf(startKey, topLevelKey)
            }
}

@Composable
fun rememberNavigationState(startKey: NavKey, topLevelKeys: List<NavKey>): NavigationState {
    val navKeySaver =
        remember(topLevelKeys) {
            Saver<MutableState<NavKey>, Int>(
                save = { state -> topLevelKeys.indexOf(state.value) },
                restore = { index -> mutableStateOf(topLevelKeys[index]) },
            )
        }
    val topLevelRoute =
        rememberSaveable(saver = navKeySaver) {
            mutableStateOf(startKey)
        }

    val backStacks =
        topLevelKeys.associateWith {
            rememberNavBackStack(it)
        }

    return remember(startKey, topLevelRoute) {
        NavigationState(
            startKey = startKey,
            topLevelKey = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries =
        backStacks.mapValues { (_, backStack) ->
            val decorators =
                listOf<NavEntryDecorator<NavKey>>(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                )
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }
    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
