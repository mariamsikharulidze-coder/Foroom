package com.example.network.training

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared storage and change notifications for the local REST and chat clients. */
class TrainingStore(context: Context) {
    internal val preferences = context.getSharedPreferences("foroom_training", Context.MODE_PRIVATE)
    internal val packageName = context.packageName
    private val updates = MutableSharedFlow<MessageEvent>(extraBufferCapacity = 64)
    internal val messages = updates.asSharedFlow()

    internal data class MessageEvent(val chatId: Int, val json: String)

    internal fun user(token: String): JSONObject {
        val name = preferences.getString("session_${token.removePrefix("Bearer ")}", null)
            ?: throw IllegalStateException("Please sign in again")
        return JSONObject(preferences.getString("users", "{}")!!).optJSONObject(name)
            ?: throw IllegalStateException("User does not exist")
    }

    internal fun requireChat(chatId: Int) {
        val chats = JSONArray(preferences.getString("chats", "[]")!!)
        require((0 until chats.length()).any { chats.getJSONObject(it).getInt("id") == chatId }) {
            "Chat does not exist"
        }
    }

    internal fun history(chatId: Int, page: Int, limit: Int, beforeId: Int?): JSONObject {
        requireChat(chatId)
        val stored = JSONArray(preferences.getString("messages_$chatId", "[]")!!)
        val items = (0 until stored.length()).map { stored.getJSONObject(it) }
            .filter { beforeId == null || it.getInt("id") < beforeId }
            .sortedByDescending { it.getInt("id") }
        val offset = if (beforeId != null) 0 else
            (page.toLong() * limit).coerceAtMost(items.size.toLong()).toInt()
        val result = JSONArray(items.drop(offset).take(limit))
        return JSONObject().put("result", result).put("hasNext", offset + result.length() < items.size)
    }

    internal suspend fun send(token: String, chatId: Int, claimedUserId: String, text: String) {
        val event = synchronized(this) {
            val user = user(token)
            require(user.getString("id") == claimedUserId) { "Message sender does not match the signed-in user" }
            requireChat(chatId)
            require(text.isNotBlank()) { "Message cannot be blank" }
            val stored = JSONArray(preferences.getString("messages_$chatId", "[]")!!)
            val id = preferences.getInt("next_message_id", 1)
            val message = JSONObject().put("id", id).put("userId", user.getString("id"))
                .put("username", user.getString("userName"))
                .put("avatarUrl", "android.resource://$packageName/drawable/training_avatar_${user.getInt("avatarId")}")
                .put("text", text)
                .put("createdAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date()))
            stored.put(message)
            check(preferences.edit().putString("messages_$chatId", stored.toString())
                .putInt("next_message_id", id + 1).commit()) { "Could not save message" }
            MessageEvent(chatId, message.toString())
        }
        updates.emit(event)
    }
}
