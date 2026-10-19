package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.component.IntSliderState
import com.ehviewer.core.ui.component.RollingNumber
import com.ehviewer.core.ui.component.RollingNumberPlaceholder
import com.ehviewer.core.ui.component.Slider
import com.ehviewer.core.ui.component.defaultMaxTickCount
import com.ehviewer.core.ui.util.HapticFeedbackType
import com.ehviewer.core.ui.util.LocalWindowSizeClass
import com.ehviewer.core.ui.util.isExpanded
import com.ehviewer.core.ui.util.rememberHapticFeedback

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    slider: IntSliderState,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    containerColor: Color,
) = CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
    val windowSizeClass = LocalWindowSizeClass.current
    val horizontalPadding = if (windowSizeClass.isExpanded) 24.dp else 16.dp
    Row(
        modifier = Modifier.padding(horizontal = horizontalPadding).clip(CircleShape).background(containerColor).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RollingNumber(slider.value, length = "$totalPages".length)
        val maxTickCount = defaultMaxTickCount()
        val interactionSource = remember { MutableInteractionSource() }
        if (slider.state.steps < maxTickCount) {
            val sliderDragged by interactionSource.collectIsDraggedAsState()
            val hapticFeedback = rememberHapticFeedback()
            LaunchedEffect(slider.value) {
                if (sliderDragged) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
                }
            }
        }
        Slider(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            state = slider,
            onValueChange = onSliderValueChange,
            maxTickCount = maxTickCount,
            interactionSource = interactionSource,
        )
        RollingNumberPlaceholder(totalPages)
    }
}
