package com.example.foroom.data.api

import com.example.foroom.data.model.ChatEntity
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ForoomApi {
    @GET("api/users/currentUser")
    suspend fun getCurrentUser(): UserEntity

    @POST("api/users/register")
    suspend fun registerUser(@Body request: RegistrationRequestEntity): UserTokenResponse

    @POST("api/users/signin")
    suspend fun logInUser(@Body request: LogInRequestEntity): UserTokenResponse

    @GET("api/chats")
    suspend fun getChats(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("name") name: String? = null,
        @Query("popular") popular: Boolean = false,
        @Query("created") created: Boolean = false,
        @Query("favorite") favorite: Boolean = false
    ): ChatsResponseEntity

    @GET("api/messages")
    suspend fun getMessageHistory(
        @Query("chatId") chatId: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("beforeId") beforeId: Int? = null,
    ): MessageHistoryResponseEntity

    @POST("api/chats")
    suspend fun createChat(@Body request: CreateChatRequest): ChatEntity

    @PUT("api/Users/ChangeAvatar")
    suspend fun changeAvatar(@Body request: ChangeUserAvatarRequest)

    @PUT("api/Users/ChangeUsername")
    suspend fun changeUsername(@Body request: ChangeUsernameRequest)

    @PUT("api/Users/ResetPassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @POST("api/Users/SignOut")
    suspend fun signOut()

    @PUT("api/chats/{id}/favorite")
    suspend fun favoriteChat(@Path("id") id: Int)

    @PUT("api/chats/{id}/unfavorite")
    suspend fun unfavoriteChat(@Path("id") id: Int)

    @DELETE("api/chats/{id}")
    suspend fun deleteChat(@Path("id") id: Int)
}