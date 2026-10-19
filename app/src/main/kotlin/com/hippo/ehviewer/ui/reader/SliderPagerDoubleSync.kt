package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.ehviewer.core.ui.component.IntSliderState
import com.hippo.ehviewer.gallery.PageLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull

@Stable
class SliderPagerDoubleSync(
    private val lazyListState: LazyListState,
    private val pagerState: PagerState,
    private val pageLoader: PageLoader,
) {
    private var sliderFollowPager by mutableStateOf(true)
    val slider = IntSliderState(pageLoader.startPage + 1, 1..pageLoader.size)

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        slider.value = index
    }

    fun reset() {
        sliderFollowPager = true
    }

    fun currentPageFlow(webtoon: Boolean) = if (webtoon) {
        snapshotFlow {
            with(lazyListState.layoutInfo) {
                visibleItemsInfo.lastOrNull {
                    it.offset <= maxOf(viewportStartOffset, viewportEndOffset - it.size)
                }?.index
            }
        }.filterNotNull()
    } else {
        snapshotFlow { pagerState.currentPage }
    }

    @Composable
    fun Sync(webtoon: Boolean, onPageSelected: () -> Unit) {
        val currentIndexFlow = remember(webtoon) {
            val initialIndex = slider.value - 1
            sliderFollowPager = if (webtoon) {
                lazyListState.firstVisibleItemIndex == initialIndex
            } else {
                pagerState.currentPage == initialIndex
            }
            currentPageFlow(webtoon)
        }
        if (sliderFollowPager) {
            LaunchedEffect(currentIndexFlow) {
                currentIndexFlow.drop(1).collect { index ->
                    slider.value = index + 1
                    pageLoader.startPage = index
                    onPageSelected()
                }
            }
        } else {
            LaunchedEffect(webtoon) {
                snapshotFlow { slider.value - 1 }.collectLatest { index ->
                    if (webtoon) {
                        lazyListState.scrollToItem(index)
                    } else {
                        pagerState.animateScrollToPage(index)
                    }
                    pageLoader.startPage = index
                }
            }
        }
    }
}

@Stable
@Composable
fun rememberSliderPagerDoubleSyncState(
    lazyListState: LazyListState,
    pagerState: PagerState,
    pageLoader: PageLoader,
): SliderPagerDoubleSync = remember {
    SliderPagerDoubleSync(lazyListState, pagerState, pageLoader)
}
