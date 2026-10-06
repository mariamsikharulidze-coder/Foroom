package com.example.foroom.domain.repository.rest

import com.example.foroom.domain.model.Chat
import com.example.foroom.domain.model.User
import com.example.foroom.domain.model.response.ChatsResponse
import com.example.foroom.domain.model.request.LogInRequest
import com.example.foroom.domain.model.request.RegistrationRequest
import com.example.foroom.domain.model.response.MessageHistoryResponse
import com.example.network.model.response.UserTokenResponse
import com.example.shared.model.Image

interface ForoomRestRepository {
    suspend fun getCurrentUser(): User

    suspend fun getAvatars(): List<Image>

    suspend fun getEmojis(): List<Image>

    suspend fun registerUser(request: RegistrationRequest): UserTokenResponse

    suspend fun logInUser(request: LogInRequest): UserTokenResponse

    suspend fun getChats(
        page: Int,
        limit: Int,
        name: String? = null,
        popular: Boolean = false,
        created: Boolean = false,
        favorite: Boolean = false
    ): ChatsResponse

    suspend fun getMessageHistory(
        currentUserId: String,
        chatId: Int,
        page: Int,
        limit: Int,
        beforeId: Int? = null
    ): MessageHistoryResponse

    suspend fun createChat(name: String, emojiId: Int): Chat

    suspend fun changeAvatar(avatarId: Int)

    suspend fun changeUsername(newUsername: String)

    suspend fun changePassword(newPassword: String)

    suspend fun signOut()

    suspend fun favoriteChat(id: Int)

    suspend fun unfavoriteChat(id: Int)
    suspend fun deleteChat(id: Int)
}