package com.example.foroom.presentation.ui.screens.home.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.design_system.components.shimmer.ForoomShimmerDrawableBuilder
import com.alternator.foroom.databinding.FragmentForoomProfileBinding
import com.example.foroom.presentation.ui.screens.home.chats.events.ForoomHomeChatsEvents
import com.example.foroom.presentation.ui.screens.home.container.events.ForoomHomeEvents
import com.example.foroom.presentation.ui.screens.home.container.events.HomeNavigationType
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_language.ForoomChangeLanguageBottomSheet
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_password.ForoomChangePasswordBottomSheet
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_profile_picture.ForoomChangeProfilePictureBottomSheet
import com.example.foroom.presentation.ui.screens.home.profile.bottom_sheets.change_username.ForoomChangeUsernameBottomSheet
import com.example.foroom.presentation.ui.screens.home.profile.events.ProfileScreenEvents
import com.example.foroom.presentation.ui.screens.log_in.ForoomLoginFragment
import com.example.navigation.host.openRootPage
import com.example.navigation.util.navigationHost
import com.example.shared.extension.handleResult
import com.example.shared.extension.loadImageUrl
import com.example.shared.extension.onClick
import com.example.shared.ui.fragment.BaseFragment
import com.example.shared.util.events.observeEvent
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForoomProfileFragment : BaseFragment<ForoomProfileViewModel, FragmentForoomProfileBinding>() {
    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> FragmentForoomProfileBinding
        get() = FragmentForoomProfileBinding::inflate
    override val viewModel: ForoomProfileViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setListeners()
        setObservers()
    }

    private fun initViews() {
        binding.userImageView.image.setImageDrawable(
            ForoomShimmerDrawableBuilder.getDefaultDrawable(requireContext())
        )
    }

    private fun setListeners() {
        binding.createdChatsItem.onClick {
            eventsHub?.sendEvent(ForoomHomeEvents.HomeNavigationEvents(HomeNavigationType.CHATS))
            eventsHub?.sendEvent(ForoomHomeChatsEvents.FilterCreatedChats)
        }

        binding.favoriteChatsItem.onClick {
            eventsHub?.sendEvent(ForoomHomeEvents.HomeNavigationEvents(HomeNavigationType.CHATS))
            eventsHub?.sendEvent(ForoomHomeChatsEvents.FilterFavouriteChats)
        }

        binding.changeUsernameItem.onClick {
            ForoomChangeUsernameBottomSheet().show(childFragmentManager, null)
        }

        binding.changePasswordItem.onClick {
            ForoomChangePasswordBottomSheet().show(childFragmentManager, null)
        }

        binding.changeLanguageItem.onClick {
            ForoomChangeLanguageBottomSheet().show(childFragmentManager, null)
        }

        binding.signOutItem.onClick {
            viewModel.signOut()
        }

        binding.userImageView.onClick {
            viewModel.remoteAvatarUrl?.let { url ->
                ForoomChangeProfilePictureBottomSheet.newInstance(url)
                    .show(childFragmentManager, null)
            }
        }
    }

    private fun setObservers() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) { user ->
            binding.userImageView.image.loadImageUrl(
                user.avatarUrl,
                ForoomShimmerDrawableBuilder.getDefaultDrawable(requireContext())
            )

            binding.userNameTextView.text = user.userName
        }

        viewModel.signOutLiveData.handleResult(viewLifecycleOwner) {
            onSuccess {
                navigationHost?.openRootPage(ForoomLoginFragment())
            }
        }

        eventsHub?.observeEvent<ProfileScreenEvents.LocalProfileImageChange>(viewLifecycleOwner) { event ->
            binding.userImageView.image.loadImageUrl(
                event.newImageUrl,
                ForoomShimmerDrawableBuilder.getDefaultDrawable(requireContext())
            )
        }

        eventsHub?.observeEvent<ProfileScreenEvents.ReloadProfile>(viewLifecycleOwner) {
            viewModel.getAndSaveUserData()
        }

        eventsHub?.observeEvent<ProfileScreenEvents.SignOut>(viewLifecycleOwner) {
            viewModel.signOut()
        }
    }
}