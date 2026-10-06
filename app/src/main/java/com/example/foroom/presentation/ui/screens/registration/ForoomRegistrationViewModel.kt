package com.example.foroom.presentation.ui.screens.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alternator.foroom.R
import com.example.foroom.domain.model.request.RegistrationRequest
import com.example.foroom.domain.usecase.GetAvatarsUseCase
import com.example.foroom.domain.usecase.RegisterUserUseCase
import com.example.foroom.presentation.ui.delegate.saveuser.GetAndSaveUserDelegate
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.shared.util.runtime.user_token.UserTokenRuntimeHolder
import com.example.network.model.response.UserTokenResponse
import com.example.network.rest_client.networkExecutor
import com.example.shared.model.Image
import com.example.shared.model.Result
import com.example.shared.ui.viewModel.BaseViewModel

class ForoomRegistrationViewModel(
    private val getAvatarsUseCase: GetAvatarsUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val userTokenRuntimeHolder: UserTokenRuntimeHolder,
    private val userDataStore: ForoomUserDataStore,
    private val getAndSaveUserDelegate: GetAndSaveUserDelegate
) : BaseViewModel(), GetAndSaveUserDelegate by getAndSaveUserDelegate {
    val avatarsLiveData = MutableLiveData<Result<List<Image>>>()

    private val _registrationLiveData = MutableLiveData<Result<UserTokenResponse>>()
    val registrationLiveData: LiveData<Result<UserTokenResponse>> get() = _registrationLiveData

    var userName: String = EMPTY_STRING
    var password: String = EMPTY_STRING
    var repeatedPassword: String = EMPTY_STRING

    var avatarId: Int = Image.BLANK_IMAGE_ID

    init {
        getAndSaveUserDelegate.init(viewModelScope)

        getImages()
    }

    private fun getImages() {
        networkExecutor {
            execute {
                getAvatarsUseCase()
            }

            onResult { result ->
                avatarsLiveData.postValue(result)
            }
        }
    }

    fun register() {
        if (avatarId == Image.BLANK_IMAGE_ID) {
            sendErrorMessage(R.string.blank_avatar_id_error)
            return
        }

        if (password != repeatedPassword) {
            sendErrorMessage(R.string.password_does_not_match_error)
            return
        }

        networkExecutor<UserTokenResponse> {
            execute {
                val response = registerUserUseCase(RegistrationRequest(userName, password, avatarId))
                userTokenRuntimeHolder.setUserToken(response.token)
                userDataStore.saveUserAuthToken(response.token)
                response
            }

            onResult { result ->
                _registrationLiveData.postValue(result)
            }

            success {
                getAndSaveUserData()
            }
        }
    }

    companion object {
        private const val EMPTY_STRING = ""
    }
}