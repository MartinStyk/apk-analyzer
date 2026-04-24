package sk.styk.martin.apkanalyzer.feature.apps.impl.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.components.SkeletonBox
import sk.styk.martin.apkanalyzer.core.uilibrary.lazylist.ListItemPosition
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.card
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.shimmer
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes

private val SKELETON_ICON_SIZE = 44.dp
private val SKELETON_RECENT_ICON_SIZE = 56.dp

@Composable
internal fun AppListItemRowSkeleton(position: ListItemPosition, modifier: Modifier = Modifier) {
    val shape = when (position) {
        ListItemPosition.Single -> Shapes.CardShape
        ListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ListItemPosition.Middle -> RectangleShape
        ListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SKELETON_ICON_SIZE)
                .clip(Shapes.CardShape)
                .background(AppTheme.colors.surfaceVariant)
                .shimmer(),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(11.dp),
            )
        }
    }
}

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
