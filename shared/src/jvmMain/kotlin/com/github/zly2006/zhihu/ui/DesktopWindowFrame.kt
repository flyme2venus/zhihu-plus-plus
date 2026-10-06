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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.github.zly2006.zhihu.theme.ThemeManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

/**
 * 桌面端自绘窗口框架：自绘标题栏（拖拽、双击最大化、最小化/最大化/关闭）+ 应用内容。
 * 必须配合 `Window(undecorated = true)` 使用，窗口边缘缩放由 Compose 的
 * UndecoratedWindowResizer 在窗口层自动处理。
 * 标题栏颜色与页面背景一致（纯黑模式下为真黑）。
 */
@Composable
fun FrameWindowScope.DesktopWindowFrame(
    icon: Painter,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val isAmoled = ThemeManager.getIsAmoled()
    val darkTheme = ThemeManager.isDarkTheme()
    val titleBarColor = if (isAmoled && darkTheme) Color.Black else ThemeManager.getBackgroundColor()
    val glyphColor = MaterialTheme.colorScheme.onSurface
    val window = window

    // 最大化状态可能被 Win+方向键等系统操作改变，用组件事件跟踪
    var isMaximized by remember {
        mutableStateOf(window.extendedState and java.awt.Frame.MAXIMIZED_BOTH != 0)
    }
    DisposableEffect(window) {
        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                isMaximized = window.extendedState and java.awt.Frame.MAXIMIZED_BOTH != 0
            }
        }
        window.addComponentListener(listener)
        onDispose { window.removeComponentListener(listener) }
    }

    Column(Modifier.fillMaxSize().background(titleBarColor)) {
        WindowDraggableArea {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(titleBarColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                window.extendedState = window.extendedState xor java.awt.Frame.MAXIMIZED_BOTH
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 12.dp).size(16.dp),
                )
                Text(
                    text = "Zhihu++",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                WindowCaptionButton(
                    onClick = {
                        window.extendedState = window.extendedState or java.awt.Frame.ICONIFIED
                    },
                    normalColor = glyphColor,
                    hoverBackground = glyphColor.copy(alpha = 0.08f),
                    hoverColor = glyphColor,
                ) { color ->
                    MinimizeGlyph(color)
                }
                WindowCaptionButton(
                    onClick = {
                        window.extendedState = window.extendedState xor java.awt.Frame.MAXIMIZED_BOTH
                    },
                    normalColor = glyphColor,
                    hoverBackground = glyphColor.copy(alpha = 0.08f),
                    hoverColor = glyphColor,
                ) { color ->
                    if (isMaximized) RestoreGlyph(color) else MaximizeGlyph(color)
                }
                WindowCaptionButton(
                    onClick = onCloseRequest,
                    normalColor = glyphColor,
                    hoverBackground = CloseButtonHoverRed,
                    hoverColor = Color.White,
                ) { color ->
                    CloseGlyph(color)
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

private val CloseButtonHoverRed = Color(0xFFE81123)

/** Windows 风格的标题栏按钮：固定宽度，悬停时整块变色（关闭键为红色）。 */
@Composable
private fun WindowCaptionButton(
    onClick: () -> Unit,
    normalColor: Color,
    hoverBackground: Color,
    hoverColor: Color,
    glyph: @Composable (Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(40.dp)
            .background(if (hovered) hoverBackground else Color.Transparent)
            .hoverable(interactionSource)
            .pointerInput(onClick) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        glyph(if (hovered) hoverColor else normalColor)
    }
}

@Composable
private fun MinimizeGlyph(color: Color) {
    Canvas(Modifier.size(10.dp, 10.dp)) {
        val y = size.height - 1.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

@Composable
private fun MaximizeGlyph(color: Color) {
    Canvas(Modifier.size(10.dp, 10.dp)) {
        val inset = 1.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(1.dp.toPx()),
        )
    }
}

@Composable
private fun RestoreGlyph(color: Color) {
    Canvas(Modifier.size(10.dp, 10.dp)) {
        val stroke = 1.dp.toPx()
        val inset = 1.dp.toPx()
        val offset = 2.5f
        // 背景窗口：顶边 + 右边
        drawLine(color, Offset(offset, offset), Offset(size.width - inset, offset), stroke)
        drawLine(color, Offset(size.width - inset, offset), Offset(size.width - inset, size.height - offset), stroke)
        // 前景窗口
        drawRect(
            color = color,
            topLeft = Offset(inset, offset + inset),
            size = Size(size.width - offset - inset, size.height - offset - inset),
            style = Stroke(stroke),
        )
    }
}

@Composable
private fun CloseGlyph(color: Color) {
    Canvas(Modifier.size(10.dp, 10.dp)) {
        val stroke = 1.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), stroke)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke)
    }
}
