package com.example.foroom.presentation.ui.delegate.sign_out

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.foroom.domain.usecase.RemoteSignOutUseCase
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.network.rest_client.parentNetworkExecutor
import com.example.shared.ui.delegate.BaseForoomDelegate
import com.example.shared.model.Result
import com.example.shared.util.runtime.user_token.UserTokenRuntimeHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class SignOutDelegateImpl(
    private val remoteSignOutUseCase: RemoteSignOutUseCase,
    private val userDataStore: ForoomUserDataStore,
    private val tokens: UserTokenRuntimeHolder
): SignOutDelegate, BaseForoomDelegate() {
    private val _signOutLiveData = MutableLiveData<Result<Unit>>()
    override val signOutLiveData: LiveData<Result<Unit>> get() = _signOutLiveData

    private var signOutJob: Job? = null

    override fun signOut() {
        if (signOutJob?.isActive == true) return
        signOutJob = parentNetworkExecutor {
            execute {
                try {
                    remoteSignOutUseCase()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    // Local sign-out must still work when the server is unavailable.
                }
                userDataStore.clearUserData()
                tokens.setUserToken("")
            }

            onResult { result ->
                _signOutLiveData.postValue(result)
            }
        }
    }
}