package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Mr D's grey, rounded search field. The caller owns [state] (spec section 4.4 keeps it in the ViewModel). A clear
 * button appears once there's text, and the keyboard's search key closes the keyboard: results update as you type.
 */
@Composable
fun MrdSearchField(state: TextFieldState, placeholder: String, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.onSurface),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboard?.hide() },
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(colors.surfaceVariant, MrdShapes.Field)
                    .heightIn(min = 48.dp)
                    .padding(start = MrdSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MrdIcons.Search, contentDescription = null, tint = colors.onSurface)
                Box(Modifier.weight(1f).padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.Medium)) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                    innerTextField()
                }
                if (state.text.isNotEmpty()) {
                    IconButton(onClick = { state.clearText() }) {
                        Icon(
                            MrdIcons.Close,
                            contentDescription = stringResource(R.string.designsystem_clear_search),
                            tint = colors.onSurface
                        )
                    }
                } else {
                    Spacer(Modifier.width(MrdSpacing.Medium))
                }
            }
        }
    )
}

@MrdPreviews
@Composable
private fun MrdSearchFieldPreview() {
    MrdTheme {
        Surface {
            MrdSearchField(rememberTextFieldState(), "Search restaurants", Modifier.padding(MrdSpacing.Large))
        }
    }
}
