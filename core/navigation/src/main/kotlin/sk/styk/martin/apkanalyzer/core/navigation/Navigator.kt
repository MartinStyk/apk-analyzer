package sk.styk.martin.apkanalyzer.core.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val navigationState: NavigationState) {
    fun navigate(key: NavKey) {
        when (key) {
            in navigationState.backStacks.keys -> navigationState.topLevelKey = key
            else -> navigationState.backStacks[navigationState.topLevelKey]?.add(key)
        }
    }

    fun goBack() {
        val currentStack = navigationState.backStacks[navigationState.topLevelKey] ?: error("Backstack for ${navigationState.topLevelKey} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == navigationState.topLevelKey) {
            navigationState.topLevelKey = navigationState.startKey
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
