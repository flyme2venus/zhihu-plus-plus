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

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

/**
 * Windows 桌面端窗口 chrome 调整：通过 DWM 让系统标题栏颜色跟随主题背景。
 *
 * - Win11 (build 22000+)：DWMWA_CAPTION_COLOR / DWMWA_TEXT_COLOR / DWMWA_BORDER_COLOR
 *   精确上色，纯黑模式下标题栏为真黑；
 * - Win10：精确上色不可用，退回 DWMWA_USE_IMMERSIVE_DARK_MODE（深色标题栏）；
 * - 非 Windows 平台为 no-op。
 */
object DesktopWindowChrome {
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    // 方法名必须与 dwmapi 导出函数名一致，JNA 按名映射
    @Suppress("ktlint:standard:function-naming")
    private interface DwmApi : StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: Pointer, size: Int): Int
    }

    /**
     * 让标题栏跟随主题。[backgroundArgb] 为页面背景色（ARGB），[dark] 决定标题文字颜色。
     * 需在窗口可显示（peer 已创建）后调用；主题变化时重复调用即可。
     */
    fun applyCaptionColor(window: Window, backgroundArgb: Int, dark: Boolean) {
        if (!System.getProperty("os.name").lowercase().contains("win")) return
        try {
            val hwnd = Native.getComponentPointer(window) ?: return
            val dwm = Native.load("dwmapi", DwmApi::class.java)
            val colorRef = toColorRef(backgroundArgb)
            val border = Memory(4).apply { setInt(0, colorRef) }
            val caption = Memory(4).apply { setInt(0, colorRef) }
            val text = Memory(4).apply { setInt(0, if (dark) 0xFFFFFF else 0x000000) }
            val immersive = Memory(4).apply { setInt(0, if (dark) 1 else 0) }

            // 先设深色模式再上色：后设 immersive dark 会重置已设置的 caption 颜色
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, immersive, 4)
            val captionResult = dwm.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, caption, 4)
            if (captionResult != 0) {
                // Win11 以下不支持精确上色；dark 时 immersive 已生效，浅色下恢复默认
                if (!dark) {
                    dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, immersive, 4)
                }
                com.github.zly2006.zhihu.util.Log.e(
                    "DesktopWindowChrome",
                    "DWMWA_CAPTION_COLOR failed: $captionResult",
                )
            } else {
                dwm.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, border, 4)
                dwm.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, text, 4)
            }
        } catch (_: Throwable) {
            // 标题栏上色失败不影响应用功能
        }
    }

    /** ARGB → COLORREF（0x00BBGGRR）。 */
    private fun toColorRef(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (b shl 16) or (g shl 8) or r
    }
}
