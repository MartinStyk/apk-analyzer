package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.annotation.StringRes
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableList

@Stable
data class NavigationBarItem(val navKey: NavKey, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, @StringRes val title: Int)

@Composable
fun NavigationBar(items: ImmutableList<NavigationBarItem>, selectedKey: NavKey, onSelectKey: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    BottomAppBar(
        modifier = modifier,
    ) {
        items.forEach { item ->
            val isSelected = item.navKey == selectedKey
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectKey(item.navKey) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(id = item.title),
                    )
                },
                label = {
                    Text(stringResource(item.title))
                },
            )
        }
    }
}
