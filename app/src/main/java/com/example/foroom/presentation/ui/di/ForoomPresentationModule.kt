package com.example.foroom.presentation.ui.di

import com.example.foroom.domain.model.Chat
import com.example.foroom.domain.model.Message
import com.example.foroom.presentation.ui.activity.ForoomActivityViewModel
import com.example.foroom.presentation.ui.delegate.saveuser.GetAndSaveUserDelegate
import com.example.foroom.presentation.ui.delegate.saveuser.GetAndSaveUserDelegateImpl
import com.example.foroom.presentation.ui.delegate.sign_out.SignOutDelegate
import com.example.foroom.presentation.ui.delegate.sign_out.SignOutDelegateImpl
import com.example.foroom.presentation.ui.screens.chat.ForoomChatViewModel
import com.example.foroom.presentation.ui.screens.home.chats.ForoomHomeChatsViewModel
import com.example.foroom.presentation.ui.screens.home.container.ForoomHomeContainerViewModel
import com.example.foroom.presentation.ui.screens.create_chat.ForoomCreateChatViewModel
import com.example.foroom.presentation.ui.screens.home.profile.ForoomProfileViewModel
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_language.ForoomChangeLanguageViewModel
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_profile_picture.ForoomChangeProfilePictureViewModel
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_username.ForoomChangeUsernameViewModel
import com.example.foroom.presentation.ui.screens.log_in.ForoomLoginViewModel
import com.example.foroom.presentation.ui.screens.registration.ForoomRegistrationViewModel
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStoreImpl
import com.example.shared.util.pagination.PaginationHelper
import com.example.shared.util.pagination.PaginationHelperImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule get() = module {
    includes(registrationModule, logInModule, homeModule)

    factory<PaginationHelper<Chat>> { PaginationHelperImpl() }
    factory<PaginationHelper<Message>> { PaginationHelperImpl() }

    single<ForoomUserDataStore> {
        ForoomUserDataStoreImpl(get())
    }

    // delegate
    factory<GetAndSaveUserDelegate> {
        GetAndSaveUserDelegateImpl(get(), get())
    }

    factory<SignOutDelegate> {
        SignOutDelegateImpl(get(), get(), get())
    }

    // activity
    viewModelOf(::ForoomActivityViewModel)
}

val registrationModule = module {
    viewModelOf(::ForoomRegistrationViewModel)
}

val logInModule = module {
    viewModelOf(::ForoomLoginViewModel)
}

val homeModule = module {
    viewModelOf(::ForoomHomeContainerViewModel)

    // home chats
    viewModelOf(::ForoomHomeChatsViewModel)

    // profile
    viewModelOf(::ForoomProfileViewModel)

    // create chat
    viewModelOf(::ForoomCreateChatViewModel)

    // chat
    viewModel { params ->
        ForoomChatViewModel(chatId = params.get(), get(), get(), get(), get())
    }

    // bottom sheets
    viewModelOf(::ForoomChangeUsernameViewModel)
    viewModelOf(::ForoomChangeLanguageViewModel)
    viewModelOf(::ForoomChangeProfilePictureViewModel)
}
