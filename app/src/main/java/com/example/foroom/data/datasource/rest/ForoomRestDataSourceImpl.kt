package com.example.foroom.data.datasource.rest

import com.example.foroom.data.api.ImagesApi
import com.example.foroom.data.api.ForoomApi
import com.example.foroom.data.model.ChatEntity
import com.example.foroom.data.model.Image
import com.example.foroom.data.model.UserEntity
import com.example.foroom.data.model.request.ChangePasswordRequest
import com.example.foroom.data.model.request.ChangeUserAvatarRequest
import com.example.foroom.data.model.request.ChangeUsernameRequest
import com.example.foroom.data.model.request.CreateChatRequest
import com.example.foroom.data.model.request.LogInRequestEntity
import com.example.foroom.data.model.request.RegistrationRequestEntity
import com.example.foroom.data.model.response.ChatsResponseEntity
import com.example.foroom.data.model.response.MessageHistoryResponseEntity
import com.example.network.model.response.UserTokenResponse

class ForoomRestDataSourceImpl(
    private val imagesApi: ImagesApi,
    private val foroomApi: ForoomApi
) : ForoomRestDataSource {

    override suspend fun getCurrentUser(): UserEntity {
        return foroomApi.getCurrentUser()
    }

    override suspend fun getAvatars(): List<Image> {
        return imagesApi.getAvatars()
    }

    override suspend fun getEmojis(): List<Image> {
        return imagesApi.getEmojis()
    }

    override suspend fun registerUser(request: RegistrationRequestEntity): UserTokenResponse {
        return foroomApi.registerUser(request)
    }

    override suspend fun logInUser(request: LogInRequestEntity): UserTokenResponse {
        return foroomApi.logInUser(request)
    }

    override suspend fun getChats(
        page: Int,
        limit: Int,
        name: String?,
        popular: Boolean,
        created: Boolean,
        favorite: Boolean
    ): ChatsResponseEntity {
        return foroomApi.getChats(page, limit, name, popular, created, favorite)
    }

    override suspend fun getMessageHistory(chatId: Int, page: Int, limit: Int, beforeId: Int?): MessageHistoryResponseEntity {
        return foroomApi.getMessageHistory(chatId, page, limit, beforeId)
    }

    override suspend fun createChat(name: String, emojiId: Int): ChatEntity {
        return foroomApi.createChat(CreateChatRequest(name, emojiId))
    }

    override suspend fun changeAvatar(avatarId: Int) {
        foroomApi.changeAvatar(ChangeUserAvatarRequest(avatarId))
    }

    override suspend fun changeUsername(newUsername: String) {
        foroomApi.changeUsername(ChangeUsernameRequest(newUsername))
    }

    override suspend fun changePassword(newPassword: String) {
        foroomApi.changePassword(ChangePasswordRequest(newPassword))
    }

    override suspend fun signOut() {
        foroomApi.signOut()
    }

    override suspend fun favoriteChat(id: Int) {
        foroomApi.favoriteChat(id)
    }

    override suspend fun unfavoriteChat(id: Int) {
        foroomApi.unfavoriteChat(id)
    }

    override suspend fun deleteChat(id: Int) {
        foroomApi.deleteChat(id)
    }
}