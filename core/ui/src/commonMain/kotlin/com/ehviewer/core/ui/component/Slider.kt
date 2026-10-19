package com.ehviewer.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.SliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastRoundToInt
import com.ehviewer.core.preferences.PrefDelegate

class IntSliderState(val state: SliderState) {
    constructor(value: Int, range: IntRange, steps: Int = range.last - range.first - 1) : this(SliderState(value.toFloat(), steps, range.first.toFloat()..range.last.toFloat()))
    var value: Int
        get() = state.value.fastRoundToInt()
        set(value) {
            state.value = value.toFloat()
        }
}

@Composable
fun rememberIntSliderState(value: Int, range: IntRange, steps: Int = range.last - range.first - 1): IntSliderState {
    val state = rememberSliderState(value.toFloat(), steps, range.first.toFloat()..range.last.toFloat())
    return remember(range, steps) { IntSliderState(state) }
}

@Composable
fun rememberIntPrefSliderState(value: PrefDelegate<Int>, range: IntRange, steps: Int = range.last - range.first - 1) = rememberIntSliderState(value.value, range, steps).also { value.value = it.value }

@Composable
fun Slider(
    state: IntSliderState,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxTickCount: Int = defaultMaxTickCount(),
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Slider(
        state = state.state,
        onValueChange = { onValueChange(it.fastRoundToInt()) },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                trackCornerSize = Dp.Unspecified,
                enabled = enabled,
                colors = colors,
                drawTick = if (state.steps < maxTickCount) {
                    { offset, color ->
                        drawStopIndicator(offset = offset, color = color, size = SliderDefaults.TickSize)
                    }
                } else {
                    NoOpDrawTick
                },
            )
        },
    )
}

@Composable
fun defaultMaxTickCount() = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width / (SliderDefaults.TickSize.toPx() * 2.5f).toInt()
}

private val NoOpDrawTick: DrawScope.(Offset, Color) -> Unit = { _, _ -> }
