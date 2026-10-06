package com.example.foroom.data.datasource.rest

import com.example.foroom.data.model.ChatEntity
import com.example.foroom.data.model.Image
import com.example.foroom.data.model.UserEntity
import com.example.foroom.data.model.request.LogInRequestEntity
import com.example.foroom.data.model.request.RegistrationRequestEntity
import com.example.foroom.data.model.response.ChatsResponseEntity
import com.example.foroom.data.model.response.MessageHistoryResponseEntity
import com.example.network.model.response.UserTokenResponse

interface ForoomRestDataSource {
    suspend fun getCurrentUser(): UserEntity

    suspend fun getAvatars(): List<Image>

    suspend fun getEmojis(): List<Image>

    suspend fun registerUser(request: RegistrationRequestEntity): UserTokenResponse

    suspend fun logInUser(request: LogInRequestEntity): UserTokenResponse

    suspend fun getChats(
        page: Int,
        limit: Int,
        name: String? = null,
        popular: Boolean = false,
        created: Boolean = false,
        favorite: Boolean = false
    ): ChatsResponseEntity

    suspend fun getMessageHistory(
        chatId: Int,
        page: Int,
        limit: Int,
        beforeId: Int? = null
    ): MessageHistoryResponseEntity

    suspend fun createChat(name: String, emojiId: Int): ChatEntity

    suspend fun changeAvatar(avatarId: Int)

    suspend fun changeUsername(newUsername: String)

    suspend fun changePassword(newPassword: String)

    suspend fun signOut()

    suspend fun favoriteChat(id: Int)

    suspend fun unfavoriteChat(id: Int)

    suspend fun deleteChat(id: Int)
}