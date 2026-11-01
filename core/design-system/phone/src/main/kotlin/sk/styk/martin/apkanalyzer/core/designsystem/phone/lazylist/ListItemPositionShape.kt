package sk.styk.martin.apkanalyzer.core.designsystem.phone.lazylist

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes

fun ListItemPosition.shape(): Shape = when (this) {
    ListItemPosition.Single -> Shapes.CardShape
    ListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ListItemPosition.Middle -> RectangleShape
    ListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
}
