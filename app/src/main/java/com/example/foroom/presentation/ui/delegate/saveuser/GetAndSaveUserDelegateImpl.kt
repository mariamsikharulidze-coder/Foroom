package com.example.foroom.presentation.ui.delegate.saveuser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.foroom.domain.model.User
import com.example.foroom.domain.usecase.GetCurrentUserUseCase
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.network.rest_client.parentNetworkExecutor
import com.example.shared.model.Result
import com.example.shared.ui.delegate.BaseForoomDelegate

class GetAndSaveUserDelegateImpl(
    private val userDataStore: ForoomUserDataStore,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : GetAndSaveUserDelegate, BaseForoomDelegate() {
    private val _getAndSaveUserResultLiveData = MutableLiveData<Result<User>>()
    override val getAndSaveUserResultLiveData: LiveData<Result<User>> get() = _getAndSaveUserResultLiveData

    override fun getAndSaveUserData() {
        parentNetworkExecutor {
            execute {
                val user = getCurrentUserUseCase()
                userDataStore.saveUser(user)
                user
            }

            onResult { result ->
                _getAndSaveUserResultLiveData.postValue(result)
            }
        }
    }

}