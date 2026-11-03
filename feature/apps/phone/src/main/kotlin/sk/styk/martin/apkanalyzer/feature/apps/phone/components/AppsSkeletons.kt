package sk.styk.martin.apkanalyzer.feature.apps.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.card
import sk.styk.martin.apkanalyzer.core.designsystem.phone.modifier.shimmer

private val SKELETON_RECENT_ICON_SIZE = 56.dp

@Composable
internal fun RecentAppsRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
        modifier = modifier
            .fillMaxWidth()
            .card(),
    ) {
        items(count = 4, key = { "recent_skeleton_$it" }) {
            RecentAppItemSkeleton()
        }
    }
}

@Composable
private fun RecentAppItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(80.dp)
            .padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(SKELETON_RECENT_ICON_SIZE)
                .clip(CircleShape)
                .background(AppTheme.colors.surfaceVariant)
                .shimmer(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(11.dp),
        )
    }
}
