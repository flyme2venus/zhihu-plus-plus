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

package com.github.zly2006.zhihu.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.account.DEFAULT_ZHIHU_USER_AGENT
import com.github.zly2006.zhihu.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.account.ZhihuAccountSession
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.account.currentAndroidZhihuAccountStore
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.ZhihuJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@SuppressLint("StaticFieldLeak")
object AccountData {
    val json = ZhihuJson.json

    // 常量本体在 commonMain 的 ZhihuAndroidApi，桌面端复用同一套请求头。
    internal val ANDROID_HEADERS = ZhihuAndroidApi.HEADERS

    val ANDROID_USER_AGENT = ZhihuAndroidApi.USER_AGENT

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: MutableMap<String, String> = mutableMapOf(),
        val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
        val self: Person? = null,
        val mobileAccessToken: String? = null,
        val mobileRefreshToken: String? = null,
        val mobileTokenType: String? = null,
        val mobileTokenExpiresAt: Long? = null,
    )

    fun loadData(context: Context): Data {
        androidZhihuAccountStore(context)
        return data
    }

    val data: Data
        get() = currentAndroidZhihuAccountStore().session.toAndroidData()

    fun saveData(context: Context, data: Data) {
        androidZhihuAccountStore(context).save(data.toSession())
    }

    private fun Data.toSession(): ZhihuAccountSession = ZhihuAccountSession(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        profile = self?.let {
            ZhihuAccountProfileSnapshot(
                id = it.id,
                name = it.name,
                urlToken = it.urlToken,
                userType = it.userType,
                avatarUrl = it.avatarUrl,
            )
        },
        self = self?.let { json.encodeToJsonElement(it) },
        mobileAccessToken = mobileAccessToken,
        mobileRefreshToken = mobileRefreshToken,
        mobileTokenType = mobileTokenType,
        mobileTokenExpiresAt = mobileTokenExpiresAt,
    )

    private fun ZhihuAccountSession.toAndroidData(): Data = Data(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        self = self?.let {
            runCatching {
                ZhihuJson.decodeJson<Person>(it)
            }.getOrNull()
        },
        mobileAccessToken = mobileAccessToken,
        mobileRefreshToken = mobileRefreshToken,
        mobileTokenType = mobileTokenType,
        mobileTokenExpiresAt = mobileTokenExpiresAt,
    )

    /**
     * 将snake_case的JSON转换为camelCase并解析为对象
     */
    internal inline fun <reified T> decodeJson(json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement<T>(convertedJson)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $convertedJson", e)
            throw SerializationException("Failed to parse JSON: ${e.message}\n\n$convertedJson", e)
        }
    }

    class ZhPlusJsonSerializationException(
        val originalJson: JsonElement,
        message: String,
        cause: Throwable?,
    ) : SerializationException(message, cause)

    internal fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement(serializer, convertedJson)
        } catch (e: SerializationException) {
            throw ZhPlusJsonSerializationException(convertedJson, "Failed to parse JSON: ${e.message}", e)
        }
    }
}
