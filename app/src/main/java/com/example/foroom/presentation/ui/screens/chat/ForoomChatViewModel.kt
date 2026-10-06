package com.example.foroom.presentation.ui.screens.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.foroom.domain.model.Message
import com.example.foroom.domain.model.response.MessageHistoryResponse
import com.example.foroom.domain.usecase.GetMessageHistoryUseCase
import com.example.foroom.domain.usecase.MessageWebSocketUseCase
import com.example.foroom.presentation.ui.model.MessageUI
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.network.BuildConfig
import com.example.network.rest_client.networkExecutor
import com.example.shared.extension.isSuccess
import com.example.shared.model.Result
import com.example.shared.ui.viewModel.BaseViewModel
import com.example.shared.util.pagination.PaginationHelper
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ForoomChatViewModel(
    private val chatId: Int,
    private val messagesUseCase: MessageWebSocketUseCase,
    private val userDataStore: ForoomUserDataStore,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
    private val paginationHelper: PaginationHelper<Message>
) : BaseViewModel() {

    private var userId: String? = null
    private var connectionJob: Job? = null
    private var messagesJob: Job? = null
    private var historyJob: Job? = null
    private var historyLoading = false

    private val _messagesLiveData = MutableLiveData<List<MessageUI>>()
    val messagesLiveData: LiveData<List<MessageUI>> get() = _messagesLiveData
    private val newMessages = mutableListOf<Message>()

    private val _connectionLiveData = MutableLiveData<Result<Unit>>()
    val connectionLiveData: LiveData<Result<Unit>> get() = _connectionLiveData

    var hasMoreMessages = false

    fun connect() {
        disConnect()
        paginationHelper.clear()
        newMessages.clear()
        hasMoreMessages = true
        historyLoading = false
        _messagesLiveData.value = emptyList()
        _connectionLiveData.value = Result.Loading
        connectionJob = viewModelScope.launch {
            userId = userDataStore.getUser().first().id
            messagesUseCase.connect().collect { result ->
                if (result.isSuccess) {
                    messagesJob = launch(start = CoroutineStart.UNDISPATCHED) {
                        messagesUseCase.onMessageReceived(requireNotNull(userId)).collect { message ->
                            newMessages.add(message)
                            combineMessages()
                        }
                    }
                    messagesUseCase.joinGroup(chatId.toString()).collect { joined ->
                        _connectionLiveData.value = joined
                        if (joined.isSuccess) getMessageHistory()
                    }
                } else _connectionLiveData.value = result
            }
        }
    }

    fun disConnect() {
        connectionJob?.cancel()
        messagesJob?.cancel()
        historyJob?.cancel()
        messagesUseCase.disconnect()
        userId = null
        historyLoading = false
    }

    fun sendMessage(text: String): Flow<Result<Unit>> {
        val id = userId
        if (id == null || _connectionLiveData.value?.isSuccess != true) {
            return flowOf(Result.Error(IllegalStateException("Chat is not ready")))
        }
        if (text.isBlank()) return flowOf(Result.Error(IllegalArgumentException("Message cannot be blank")))
        return messagesUseCase.sendMessage(id, chatId, text)
    }

    fun getMessageHistory() {
        val id = userId ?: return
        if (historyLoading || !hasMoreMessages) return
        historyLoading = true
        val page = paginationHelper.getPage()
        val beforeId = if (BuildConfig.TRAINING_MODE) paginationHelper.getItems().lastOrNull()?.id else null
        historyJob = networkExecutor<MessageHistoryResponse> {
            execute { getMessageHistoryUseCase(id, chatId, page, beforeId = beforeId) }
            onResult { result ->
                if (result is Result.Error) viewModelScope.launch { historyLoading = false }
            }
            success { response ->
                paginationHelper.addPage(response.messages)
                hasMoreMessages = response.hasNext
                historyLoading = false
                combineMessages()
            }
        }
    }

    private fun combineMessages() {
        val combined = (newMessages + paginationHelper.getItems()).distinctBy { it.id }.sortedByDescending { it.id }

        _messagesLiveData.postValue(mapToMessageUI(combined))
    }

    private fun mapToMessageUI(messages: List<Message>): List<MessageUI> {
        return messages.mapIndexed { index, message ->
            with(message) {
                val isMerged = messages.getOrNull(index - 1)?.senderUserId == senderUserId

                MessageUI(
                    senderName,
                    senderAvatarUrl,
                    sendDate,
                    text,
                    senderUserId,
                    id,
                    isCurrentUser,
                    isMerged
                )
            }
        }
    }
}
