package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.sharedElement
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun SearchBarActive(
    query: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    sharedElementKey: Any? = "search-bar",
    onQueryChange: (String) -> Unit = {},
) {
    val sharedElementModifier = if (sharedElementKey == null) modifier else modifier.sharedElement(key = sharedElementKey)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = sharedElementModifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(
                color = AppTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = ApkAnalyzerIcons.Search,
            contentDescription = null,
            tint = if (query.isEmpty()) AppTheme.colors.onSurfaceVariant else AppTheme.colors.onBackground,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            val textStyle = AppTheme.typography.bodyLarge.copy(color = AppTheme.colors.onSurface)
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
            }
            BasicTextField(
                value = textFieldValue.copy(text = query),
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != query) onQueryChange(newValue.text)
                },
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(AppTheme.colors.onBackground),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                imageVector = ApkAnalyzerIcons.Clear,
                onClick = { onQueryChange("") },
                style = IconButtonStyle.Standard,
                modifier = Modifier.padding(end = 8.dp).size(32.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SearchBarActiveEmptyPreview() {
    ApkAnalyzerTheme {
        SearchBarActive(
            query = "",
            onQueryChange = {},
            placeholder = "Search 342 apps…",
        )
    }
}

@Preview
@Composable
private fun SearchBarActiveWithQueryPreview() {
    ApkAnalyzerTheme {
        SearchBarActive(
            query = "instagram",
            onQueryChange = {},
            placeholder = "Search apps",
        )
    }
}
