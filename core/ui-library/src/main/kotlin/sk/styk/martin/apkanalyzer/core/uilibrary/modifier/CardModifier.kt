package sk.styk.martin.apkanalyzer.core.uilibrary.modifier

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shape

@Composable
fun Modifier.card(shape: Shape = Shape.CardShape): Modifier = this
    .clip(shape)
    .background(AppTheme.colors.surface, shape)
