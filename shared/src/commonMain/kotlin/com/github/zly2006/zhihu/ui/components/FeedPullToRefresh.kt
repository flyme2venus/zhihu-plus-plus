/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.isJvm
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch

val LocalPullToRefreshViewModel = compositionLocalOf<BaseFeedViewModel?> {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    FeedPullToRefresh(
        viewModel = viewModel,
        environment = rememberPaginationEnvironment(viewModel.allowGuestAccess),
        padding = padding,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    environment: PaginationEnvironment,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val settings = rememberSettingsStore()
    val pullToRefreshEnabled = settings.getBoolean("pullToRefreshEnabled", !isJvm)
    if (!pullToRefreshEnabled) {
        CompositionLocalProvider(LocalPullToRefreshViewModel provides viewModel) {
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
        return
    }
    if (isJvm) {
        // 桌面端：material3 的 PullToRefreshBox 只响应触摸，鼠标拖拽/滚轮都无法触发，
        // 改用自绘的鼠标下拉手势（不消费事件，不影响点击与文本交互）。
        DesktopFeedPullToRefresh(viewModel, environment, content)
        return
    }
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
        onRefresh = {
            scope.launch {
                viewModel.pullToRefresh(environment)
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(padding),
                isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = state,
            )
        },
        state = state,
        modifier = Modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(LocalPullToRefreshViewModel provides viewModel) {
            content()
        }
    }
}

/**
 * 桌面端鼠标下拉刷新：按下后向下拖动展开指示器，超过阈值松手触发刷新。
 * 手势全程不消费事件，不影响卡片点击等交互（桌面端鼠标拖动本来也不滚动列表）。
 */
@Composable
private fun DesktopFeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    environment: PaginationEnvironment,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val triggerPx = with(density) { 64.dp.toPx() }
    val maxPullPx = with(density) { 96.dp.toPx() }
    var pullOffset by remember { mutableStateOf(0f) }
    val isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(viewModel, environment) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var confirmed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.changedToUp()) break
                        pullOffset = (change.position.y - down.position.y)
                            .coerceIn(0f, maxPullPx)
                        if (pullOffset >= triggerPx) confirmed = true
                    }
                    if (confirmed && !(viewModel.isPullToRefresh && viewModel.isLoading)) {
                        scope.launch { viewModel.pullToRefresh(environment) }
                    }
                    pullOffset = 0f
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            val indicatorHeight = when {
                isRefreshing -> 48.dp
                pullOffset > 0f -> with(density) { pullOffset.toDp() }
                else -> 0.dp
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(indicatorHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (isRefreshing || pullOffset >= triggerPx) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}
