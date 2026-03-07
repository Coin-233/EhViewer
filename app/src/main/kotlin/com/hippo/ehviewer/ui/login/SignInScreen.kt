package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.util.LocalWindowSizeClass
import com.ehviewer.core.ui.util.isExpanded
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.Screen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(start = true)
@Composable
fun AnimatedVisibilityScope.SignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val windowSizeClass = LocalWindowSizeClass.current

    // 提取游客模式的点击逻辑
    val enterGuestMode = {
        Settings.gallerySite.value = EhUrl.SITE_E
        Settings.needSignIn.value = false
    }

    Box(contentAlignment = Alignment.Center) {
        when {
            !windowSizeClass.isExpanded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .systemBarsPadding()
                        .padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = com.hippo.ehviewer.R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring),
                        modifier = Modifier
                            .widthIn(max = dimensionResource(id = com.hippo.ehviewer.R.dimen.single_max_width))
                            .padding(top = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring_2),
                        modifier = Modifier
                            .widthIn(max = dimensionResource(id = com.hippo.ehviewer.R.dimen.single_max_width))
                            .padding(top = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)))

                    // 将游客模式按钮改为原先登录按钮的样式
                    Button(
                        onClick = enterGuestMode,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .widthIn(max = dimensionResource(id = com.hippo.ehviewer.R.dimen.single_max_width))
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .padding(top = dimensionResource(com.hippo.ehviewer.R.dimen.keyline_margin)),
                    ) {
                        Text(text = stringResource(id = R.string.guest_mode))
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .systemBarsPadding()
                        .padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .width(dimensionResource(id = com.hippo.ehviewer.R.dimen.signinscreen_landscape_caption_frame_width))
                            .padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = com.hippo.ehviewer.R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            alignment = Alignment.Center,
                            modifier = Modifier.padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring_2),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 横屏状态下也只保留一个游客模式的主按钮
                        Button(
                            onClick = enterGuestMode,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(128.dp),
                        ) {
                            Text(text = stringResource(id = R.string.guest_mode))
                        }
                    }
                }
            }
        }
    }
}
