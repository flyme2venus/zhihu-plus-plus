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

package com.github.zly2006.zhihu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun ZhihuTheme(
    content: @Composable () -> Unit,
) {
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val darkTheme = ThemeManager.isDarkTheme()
    val isAmoled = darkTheme && ThemeManager.getIsAmoled()
    val platformDynamicColorScheme = platformDynamicColorScheme(darkTheme)

    val baseColorScheme = when {
        useDynamicColor && platformDynamicColorScheme != null -> platformDynamicColorScheme
        !useDynamicColor -> {
            dynamicColorScheme(
                seedColor = ThemeManager.getCustomColor(),
                isDark = darkTheme,
                isAmoled = isAmoled,
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 纯黑模式下覆盖整个 surface 角色族（含 surfaceContainer 系列），
    // 并让自定义背景颜色让位，否则卡片等组件仍是深灰，达不到 OLED 纯黑。
    val colorScheme = if (isAmoled) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
        )
    } else {
        baseColorScheme.copy(
            background = ThemeManager.getBackgroundColor(),
            surface = ThemeManager.getBackgroundColor(),
        )
    }

    PlatformSystemBarEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)
