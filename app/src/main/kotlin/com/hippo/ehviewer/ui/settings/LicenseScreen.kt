package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.openBrowser
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesVariant
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryActionKind
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.LicenseScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.license)) },
                navigationIcon = { NavigationIcon() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val libraries by produceLibraries(com.hippo.ehviewer.R.raw.aboutlibraries)
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
            badges = LibraryBadges(funding = false),
            variant = LibrariesVariant.Refined,
            onActionClick = { lib, kind ->
                val open = { url: String -> url.isNotBlank().also { if (it) openBrowser(url) } }
                val website = lib.website.orEmpty()
                val source = lib.scm?.url.orEmpty()
                val license = lib.licenses.firstOrNull()?.url.orEmpty()
                when (kind) {
                    LibraryActionKind.Source -> open(source)
                    LibraryActionKind.Website -> open(website)
                    LibraryActionKind.License -> open(license)
                    LibraryActionKind.Sponsor -> false
                }
            },
        )
    }
}
