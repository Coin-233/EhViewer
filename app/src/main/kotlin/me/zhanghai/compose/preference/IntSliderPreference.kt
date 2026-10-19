/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.zhanghai.compose.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ehviewer.core.ui.component.IntSliderState
import com.ehviewer.core.ui.component.Slider

@Composable
fun IntSliderPreference(
    state: IntSliderState,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    valueText: @Composable (() -> Unit)? = null,
) {
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = {
            Column {
                summary?.invoke()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        state = state,
                        onValueChange = { state.value = it },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                    if (valueText != null) {
                        val theme = LocalPreferenceTheme.current
                        Box(modifier = Modifier.padding(start = theme.horizontalSpacing)) {
                            valueText()
                        }
                    }
                }
            }
        },
    )
}
