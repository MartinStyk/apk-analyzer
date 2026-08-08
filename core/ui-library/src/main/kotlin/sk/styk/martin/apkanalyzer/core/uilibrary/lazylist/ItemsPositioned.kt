package sk.styk.martin.apkanalyzer.core.uilibrary.lazylist

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable

fun <T> LazyListScope.itemsPositioned(
    items: List<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(position: ListItemPosition, item: T) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = key,
        contentType = contentType,
    ) { index, item ->
        val position = when {
            items.size == 1 -> ListItemPosition.Single
            index == 0 -> ListItemPosition.First
            index == items.lastIndex -> ListItemPosition.Last
            else -> ListItemPosition.Middle
        }
        itemContent(position, item)
    }
}

fun LazyListScope.itemsPositioned(
    count: Int,
    key: ((index: Int) -> Any)? = null,
    contentType: (index: Int) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(position: ListItemPosition) -> Unit,
) {
    items(
        count = count,
        key = key,
        contentType = contentType,
    ) { index ->
        val position = when {
            count == 1 -> ListItemPosition.Single
            index == 0 -> ListItemPosition.First
            index == count - 1 -> ListItemPosition.Last
            else -> ListItemPosition.Middle
        }
        itemContent(position)
    }
}
