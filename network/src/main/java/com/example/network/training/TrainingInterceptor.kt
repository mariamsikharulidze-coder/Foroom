package com.example.network.training

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** Local exercise backend. Never forwards a request to the production server. */
class TrainingInterceptor(private val store: TrainingStore) : Interceptor {
    constructor(context: Context) : this(TrainingStore(context))

    private val preferences = store.preferences
    private val packageName = store.packageName

    override fun intercept(chain: Interceptor.Chain): Response = synchronized(store) {
        val request = chain.request()
        val users = JSONObject(preferences.getString("users", "{}")!!)
        if (!users.has("student")) {
            users.put("student", newUser("student", "Student123!", 1))
            save(users)
        }
        val body = request.body?.let { value ->
            val buffer = Buffer()
            value.writeTo(buffer)
            val json = buffer.readUtf8()
            if (json.isBlank()) JSONObject() else JSONObject(json)
        } ?: JSONObject()
        val token = request.header("Authorization")?.removePrefix("Bearer ")
        val currentName = token?.let { preferences.getString("session_$it", null) }
        val current = currentName?.let { users.optJSONObject(it) }
        val path = request.url.encodedPath.lowercase()
        var status = 200
        fun error(code: Int, username: String? = null, password: String? = null): JSONObject {
            status = code
            return JSONObject().apply {
                username?.let { put("usernameError", it) }
                password?.let { put("passwordError", it) }
            }
        }
        fun session(name: String): JSONObject {
            val id = UUID.randomUUID().toString()
            check(preferences.edit().putString("session_$id", name).commit()) {
                "Could not save training session"
            }
            return JSONObject().put("token", id)
        }
        val result: Any = when {
            request.method == "GET" && path in listOf("/api/avatars", "/api/emojis") -> JSONArray().apply {
                (1..6).forEach { put(JSONObject().put("id", it).put("url", avatar(it))) }
            }
            request.method == "POST" && path == "/api/users/signin" -> {
                val name = body.optString("userName")
                val user = users.optJSONObject(name)
                when {
                    user == null -> error(400, "Username does not exist", "Incorrect password")
                    user.getString("passwordHash") != hash(body.optString("password"), user.getString("salt")) ->
                        error(400, password = "Incorrect password")
                    else -> session(name)
                }
            }
            request.method == "POST" && path == "/api/users/register" -> {
                val name = body.optString("userName")
                val password = body.optString("password")
                val avatarId = body.optInt("avatarId")
                when {
                    name.isBlank() -> error(400, username = "Username is required")
                    users.has(name) -> error(400, username = "Username already exists")
                    password.length < 6 -> error(400, password = "Use at least 6 characters")
                    avatarId !in 1..6 -> error(400, username = "Select an avatar")
                    else -> {
                        users.put(name, newUser(name, password, avatarId))
                        save(users)
                        session(name)
                    }
                }
            }
            request.method == "POST" && path == "/api/users/signout" -> {
                check(preferences.edit().remove("session_$token").commit()) {
                    "Could not clear training session"
                }
                JSONObject()
            }
            current == null -> error(401)
            request.method == "PUT" && path == "/api/users/resetpassword" -> {
                val password = body.optString("newPassword")
                if (password.length < 6) error(400, password = "Use at least 6 characters")
                else {
                    val salt = UUID.randomUUID().toString()
                    current.put("salt", salt).put("passwordHash", hash(password, salt))
                    val editor = preferences.edit().putString("users", users.toString())
                    preferences.all.filter { (key, value) ->
                        key.startsWith("session_") && value == currentName
                    }.keys.forEach { editor.remove(it) }
                    check(editor.commit()) { "Could not change training password" }
                    JSONObject()
                }
            }
            request.method == "POST" && path == "/api/chats" -> {
                val name = body.optString("name").trim()
                val emojiId = body.optInt("emojiId")
                if (name.isBlank() || emojiId !in 1..6) {
                    status = 400
                    JSONObject().put("message", "Enter a chat name and select an image")
                } else {
                    val chats = readChats()
                    val nextId = (chats.maxOfOrNull { it.getInt("id") } ?: 0) + 1
                    val chat = JSONObject().put("id", nextId).put("name", name)
                        .put("emojiId", emojiId).put("creatorId", current.getString("id"))
                    chats.add(chat)
                    saveChats(chats)
                    chatResponse(chat, users, current)
                }
            }
            request.method == "GET" && path == "/api/users/currentuser" -> JSONObject()
                .put("id", current.getString("id"))
                .put("userName", currentName)
                .put("avatarUrl", avatar(current.getInt("avatarId")))
            request.method == "GET" && path == "/api/chats" -> {
                val name = request.url.queryParameter("name").orEmpty()
                val created = request.url.queryParameter("created").toBoolean()
                val favorite = request.url.queryParameter("favorite").toBoolean()
                val page = request.url.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val limit = request.url.queryParameter("limit")?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                val chats = readChats().filter {
                    it.getString("name").contains(name, ignoreCase = true) &&
                        (!created || it.getString("creatorId") == current.getString("id")) && !favorite
                }.sortedByDescending { it.getInt("id") }
                val offset = (page.toLong() * limit).coerceAtMost(chats.size.toLong()).toInt()
                val result = JSONArray()
                chats.drop(offset).take(limit).forEach { result.put(chatResponse(it, users, current)) }
                JSONObject().put("result", result).put("hasNext", offset + result.length() < chats.size)
            }
            request.method == "GET" && path == "/api/messages" -> {
                val chatId = request.url.queryParameter("chatId")?.toIntOrNull()
                if (readChats().none { it.getInt("id") == chatId }) {
                    status = 404
                    JSONObject().put("message", "Chat does not exist")
                } else {
                    val page = request.url.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val limit = request.url.queryParameter("limit")?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                    val beforeId = request.url.queryParameter("beforeId")?.toIntOrNull()
                    store.history(requireNotNull(chatId), page, limit, beforeId)
                }
            }
            else -> {
                status = 501
                JSONObject().put("message", "This feature is not available in training mode")
            }
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(status).message(if (status == 200) "OK" else "Training request failed")
            .body(result.toString().toResponseBody("application/json".toMediaType())).build()
    }

    private fun readChats(): MutableList<JSONObject> {
        val chats = JSONArray(preferences.getString("chats", "[]")!!)
        return (0 until chats.length()).map { chats.getJSONObject(it) }.toMutableList()
    }

    private fun saveChats(chats: List<JSONObject>) {
        check(preferences.edit().putString("chats", JSONArray(chats).toString()).commit()) {
            "Could not save training chats"
        }
    }

    private fun chatResponse(chat: JSONObject, users: JSONObject, current: JSONObject): JSONObject {
        val creator = users.keys().asSequence().map { users.getJSONObject(it) }
            .first { it.getString("id") == chat.getString("creatorId") }
        return JSONObject().put("id", chat.getInt("id")).put("name", chat.getString("name"))
            .put("emojiUrl", avatar(chat.getInt("emojiId")))
            .put("creatorUsername", creator.getString("userName"))
            .put("likeCount", 0).put("isFavorite", false)
            .put("createdByCurrentUser", current.getString("id") == creator.getString("id"))
    }

    private fun avatar(id: Int) = "android.resource://$packageName/drawable/training_avatar_$id"

    private fun newUser(name: String, password: String, avatarId: Int): JSONObject {
        val salt = UUID.randomUUID().toString()
        return JSONObject().put("id", UUID.randomUUID().toString()).put("userName", name)
            .put("salt", salt).put("passwordHash", hash(password, salt)).put("avatarId", avatarId)
    }

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun save(users: JSONObject) {
        check(preferences.edit().putString("users", users.toString()).commit()) {
            "Could not save training users"
        }
    }
}
