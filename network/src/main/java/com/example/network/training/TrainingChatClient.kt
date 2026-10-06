package com.example.network.training

import com.example.network.web_socket.ForoomWebSocketClient
import com.example.shared.model.Result
import com.example.shared.util.runtime.user_token.UserTokenRuntimeHolder
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TrainingChatClient(
    private val store: TrainingStore,
    private val tokens: UserTokenRuntimeHolder
) : ForoomWebSocketClient {
    private val gson = Gson()
    @Volatile private var connected = false
    @Volatile private var group: Int? = null

    private fun action(block: suspend () -> Unit): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            withContext(Dispatchers.IO) { block() }
            emit(Result.Success(Unit))
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            emit(Result.Error(error))
        }
    }

    override fun connect(): Flow<Result<Unit>> = action {
        synchronized(store) { store.user(tokens.getUserToken()) }
        connected = true
    }
    override fun disconnect() { connected = false; group = null }
    override fun <T> onReceived(dataClass: Class<T>): Flow<T> = store.messages
        .filter { connected && it.chatId == group }
        .map { gson.fromJson(it.json, dataClass) }
    override fun joinGroup(groupName: String): Flow<Result<Unit>> = action {
        check(connected) { "Chat is not connected" }
        val id = groupName.toInt()
        synchronized(store) {
            store.user(tokens.getUserToken())
            store.requireChat(id)
        }
        group = id
    }
    override fun leaveGroup(groupName: String): Flow<Result<Unit>> = action {
        if (group == groupName.toIntOrNull()) group = null
    }
    override fun sendMessage(data: Any): Flow<Result<Unit>> = action {
        check(connected) { "Chat is not connected" }
        val request = gson.toJsonTree(data).asJsonObject
        val chatId = request.get("chatId").asInt
        check(group == chatId) { "Open the chat before sending a message" }
        store.send(tokens.getUserToken(), chatId, request.get("userId").asString, request.get("text").asString)
    }
}
