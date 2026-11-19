package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * A restaurant card's shape while the list loads. Skeletons are hidden from TalkBack: the screen announces the
 * loading state once instead.
 */
@Composable
fun MrdSkeletonCard(modifier: Modifier = Modifier) {
    val alpha = pulse()
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)
    ) {
        Bone(Modifier.fillMaxWidth().aspectRatio(16f / 9f), alpha, MrdShapes.Card)
        Bone(Modifier.fillMaxWidth(0.6f).height(16.dp), alpha)
        Bone(Modifier.fillMaxWidth(0.4f).height(12.dp), alpha)
        Bone(Modifier.fillMaxWidth(0.5f).height(12.dp), alpha)
    }
}

/** A menu item's shape while the menu loads. */
@Composable
fun MrdSkeletonRow(modifier: Modifier = Modifier) {
    val alpha = pulse()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)) {
            Bone(Modifier.fillMaxWidth(0.5f).height(16.dp), alpha)
            Bone(Modifier.fillMaxWidth(0.9f).height(12.dp), alpha)
            Bone(Modifier.fillMaxWidth(0.25f).height(12.dp), alpha)
        }
        Bone(Modifier.size(64.dp), alpha, MrdShapes.Card)
    }
}

/** A slow pulse, read only while drawing, so it redraws without recomposing. */
@Composable
private fun pulse(): State<Float> = rememberInfiniteTransition(label = "skeleton").animateFloat(
    initialValue = 0.45f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
    label = "skeleton alpha"
)

@Composable
private fun Bone(modifier: Modifier, alpha: State<Float>, shape: Shape = MrdShapes.Pill) {
    val color = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.clip(shape).drawBehind { drawRect(color, alpha = alpha.value) })
}

@MrdPreviews
@Composable
private fun SkeletonsPreview() {
    MrdTheme {
        Surface {
            Column(Modifier.padding(MrdSpacing.Large)) {
                MrdSkeletonCard()
                MrdSkeletonRow()
            }
        }
    }
}
