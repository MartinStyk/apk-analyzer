package sk.styk.martin.apkanalyzer.core.designsystem.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.Icon
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.tv.modifier.pointerClickable
import sk.styk.martin.apkanalyzer.core.designsystem.tv.theme.ApkAnalyzerTvTheme
import androidx.tv.material3.ModalNavigationDrawer as TvMaterialModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem as TvMaterialNavigationDrawerItem

@Composable
fun NavigationDrawer(
    items: List<NavigationDrawerItem>,
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TvMaterialModalNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                items.forEach { item ->
                    TvMaterialNavigationDrawerItem(
                        selected = item.navKey == selectedKey,
                        onClick = { onSelectKey(item.navKey) },
                        modifier = Modifier.pointerClickable(CircleShape) { onSelectKey(item.navKey) },
                        leadingContent = { Icon(imageVector = item.icon, contentDescription = null) },
                    ) {
                        Text(text = item.title, maxLines = 1)
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.padding(start = CollapsedDrawerWidth)) {
            content()
        }
    }
}

private val CollapsedDrawerWidth = 80.dp

private data object PreviewAppsKey : NavKey

private data object PreviewBrowseKey : NavKey

@Preview(device = Devices.TV_1080p)
@Composable
private fun NavigationDrawerPreview() {
    ApkAnalyzerTvTheme {
        NavigationDrawer(
            items = listOf(
                NavigationDrawerItem(navKey = PreviewAppsKey, icon = ApkAnalyzerIcons.Apps, title = "Apps"),
                NavigationDrawerItem(navKey = PreviewBrowseKey, icon = ApkAnalyzerIcons.Browse, title = "Browse"),
            ),
            selectedKey = PreviewAppsKey,
            onSelectKey = {},
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Apps", style = AppTheme.typography.displayMedium)
            }
        }
    }
}
