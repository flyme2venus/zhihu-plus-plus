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

package com.github.zly2006.zhihu.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.zly2006.zhihu.theme.DesktopThemeSettings
import com.github.zly2006.zhihu.theme.DesktopWindowChrome
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.DesktopZhihuMain

fun main() {
    System.setProperty("java.awt.im.style", "below-the-spot")
    DesktopThemeSettings.initialize()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Zhihu++",
            icon = painterResource("desktop-icon.png"),
        ) {
            val darkTheme = ThemeManager.isDarkTheme()
            val isAmoled = ThemeManager.getIsAmoled()
            val backgroundArgb = (if (isAmoled && darkTheme) Color.Black else ThemeManager.getBackgroundColor()).toArgb()
            LaunchedEffect(darkTheme, isAmoled, backgroundArgb) {
                DesktopWindowChrome.applyCaptionColor(
                    window = window,
                    backgroundArgb = backgroundArgb,
                    dark = darkTheme,
                )
                // AWT 窗口默认白底：Compose 未绘制到的区域（如分栏间隔、首帧前）会透白
                val awtBackground = java.awt.Color(
                    (backgroundArgb shr 16) and 0xFF,
                    (backgroundArgb shr 8) and 0xFF,
                    backgroundArgb and 0xFF,
                )
                window.contentPane?.background = awtBackground
                window.background = awtBackground
            }
            ZhihuTheme {
                DesktopZhihuMain()
            }
        }
    }
}
