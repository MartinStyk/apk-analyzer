package sk.styk.martin.apkanalyzer.core.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val navigationState: NavigationState) {
    fun navigate(key: NavKey) {
        when (key) {
            navigationState.currentTopLevelKey -> clearSubStack()
            in navigationState.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    fun goBack() {
        when (navigationState.currentKey) {
            navigationState.startKey -> error("You cannot go back from the start route")

            navigationState.currentTopLevelKey -> {
                // We're at the base of the current sub stack, go back to the previous top level stack.
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
                // This is the start key. Clear the stack so it's added as the only key.
                clear()
            } else {
                // Remove it if it's already in the stack so it's added at the end.
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
