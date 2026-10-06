package com.example.foroom.presentation.ui.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.foroom.domain.model.User
import com.example.foroom.domain.usecase.GetCurrentUserUseCase
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.foroom.presentation.ui.util.exception.ForoomUnauthorizedUserException
import com.example.shared.util.runtime.user_token.UserTokenRuntimeHolder
import com.example.shared.model.ForoomLanguage
import com.example.shared.model.Result
import com.example.shared.ui.viewModel.BaseViewModel
import com.example.shared.util.runtime.user_language.UserLanguageRuntimeHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.get

class ForoomActivityViewModel(
    private val userDataStore: ForoomUserDataStore,
    private val userTokenRuntimeHolder: UserTokenRuntimeHolder,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
): BaseViewModel() {
    private val _currentUserLiveData = MediatorLiveData<Result<User>>()
    val currentUserLiveData: LiveData<Result<User>> get() = _currentUserLiveData

    private val userLanguageRuntimeHolder = get<UserLanguageRuntimeHolder>()

    private var sessionJob: Job? = null

    init {
        updateRuntimeLanguageHolder()
    }

    private fun updateRuntimeLanguageHolder() {
        viewModelScope.launch {
            val language = userDataStore.getUserLanguage()?.let { langName ->
                ForoomLanguage.fromName(langName)
            } ?: ForoomLanguage.KA

            userLanguageRuntimeHolder.setUserLanguage(language)
        }
    }

    fun refreshSession() {
        sessionJob?.cancel()
        _currentUserLiveData.value = Result.Loading
        sessionJob = viewModelScope.launch {
            try {
                val token = userDataStore.getUserAuthToken().first()
                userTokenRuntimeHolder.setUserToken(token)
                if (token.isBlank()) throw ForoomUnauthorizedUserException()
                val user = getCurrentUserUseCase()
                userDataStore.saveUser(user)
                _currentUserLiveData.value = Result.Success(user)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _currentUserLiveData.value = Result.Error(error)
            }
        }
    }

    suspend fun updateUserLanguage(language: ForoomLanguage) {
        userDataStore.saveUserLanguage(language.langName)
        userLanguageRuntimeHolder.setUserLanguage(language)
    }

    suspend fun getUserLanguage() = userDataStore.getUserLanguage()?.let { language ->
        ForoomLanguage.fromName(language)
    }
}