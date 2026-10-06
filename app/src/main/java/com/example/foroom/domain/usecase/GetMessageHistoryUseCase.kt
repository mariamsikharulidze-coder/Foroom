package com.example.foroom.domain.usecase

import com.example.foroom.domain.model.response.MessageHistoryResponse
import com.example.foroom.domain.repository.rest.ForoomRestRepository

class GetMessageHistoryUseCase(private val repository: ForoomRestRepository) {

    suspend operator fun invoke(
        currentUserId: String,
        chatId: Int,
        page: Int,
        limit: Int = DEFAULT_LIMIT,
        beforeId: Int? = null
    ): MessageHistoryResponse {
        return repository.getMessageHistory(currentUserId, chatId, page, limit, beforeId)
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
    }
}