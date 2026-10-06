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

import com.github.zly2006.zhihu.platform.desktopSettingsStore

/**
 * 桌面端主题设置加载。设置页写入的键与 Android 侧保持一致；
 * 必须在首个 Compose 窗口创建前调用，否则首帧会使用默认主题。
 */
object DesktopThemeSettings {
    fun initialize() {
        val settings = desktopSettingsStore()
        val themeModeValue = settings.getString("themeMode", ThemeMode.SYSTEM.name)
        val themeMode = try {
            ThemeMode.valueOf(themeModeValue)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        ThemeManager.load(
            ThemeSnapshot(
                useDynamicColor = settings.getBoolean("useDynamicColor", true),
                customColor = settings.getInt("customThemeColor", 0xFF2196F3.toInt()),
                backgroundColorLight = settings.getInt("backgroundColorLight", 0xFFFFFFFF.toInt()),
                backgroundColorDark = settings.getInt("backgroundColorDark", 0xFF121212.toInt()),
                themeMode = themeMode,
                isAmoled = settings.getBoolean("isAmoled", false),
            ),
        )
    }
}
