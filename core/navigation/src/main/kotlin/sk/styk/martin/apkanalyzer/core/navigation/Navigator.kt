package sk.styk.martin.apkanalyzer.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class Navigator(val navigationState: NavigationState, private val onBackAtRoot: () -> Unit = { error("You cannot go back from the start route") }) {
    fun navigate(key: NavKey) {
        when (key) {
            navigationState.currentTopLevelKey -> clearSubStack()
            in navigationState.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    val results: SharedFlow<Any>
        field = MutableSharedFlow<Any>(extraBufferCapacity = 1)

    fun goBack(result: Any) {
        results.tryEmit(result)
        goBack()
    }

    fun goBack() {
        when (navigationState.currentKey) {
            navigationState.startKey -> onBackAtRoot()

            navigationState.currentTopLevelKey -> {
                navigationState.topLevelStack.removeLastOrNull()
            }

            else -> navigationState.currentSubStack.removeLastOrNull()
        }
    }

    private fun goToKey(key: NavKey) {
        navigationState.currentSubStack.apply {
            remove(key)
            add(key)
        }
    }

    private fun goToTopLevel(key: NavKey) {
        navigationState.topLevelStack.apply {
            if (key == navigationState.startKey) {
                clear()
            } else {
                remove(key)
            }
            add(key)
        }
    }

    private fun clearSubStack() {
        navigationState.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
