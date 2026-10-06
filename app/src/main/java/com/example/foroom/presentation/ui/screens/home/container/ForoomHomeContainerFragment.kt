package com.example.foroom.presentation.ui.screens.home.container

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alternator.foroom.R
import com.alternator.foroom.databinding.FragmentForoomContainerHomeBinding
import com.example.foroom.presentation.ui.screens.home.chats.ForoomHomeChatsFragment
import com.example.foroom.presentation.ui.screens.create_chat.ForoomCreateChatFragment
import com.example.foroom.presentation.ui.screens.home.container.events.ForoomHomeEvents
import com.example.foroom.presentation.ui.screens.home.container.events.HomeNavigationType
import com.example.foroom.presentation.ui.screens.home.profile.ForoomProfileFragment
import com.example.navigation.guest.ForoomNavigationArgumentsHolder
import com.example.navigation.guest.navArgs
import com.example.navigation.host.openNextPage
import com.example.navigation.util.navigationHost
import com.example.shared.ui.fragment.BaseFragment
import com.example.shared.util.events.observeEvent
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForoomHomeContainerFragment :
    BaseFragment<ForoomHomeContainerViewModel, FragmentForoomContainerHomeBinding>(),
    ForoomNavigationArgumentsHolder<ForoomHomeContainerInitialScreenIndex> {
    override val bundle: Bundle?
        get() = arguments
    override val argClass: Class<ForoomHomeContainerInitialScreenIndex>
        get() = ForoomHomeContainerInitialScreenIndex::class.java

    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> FragmentForoomContainerHomeBinding
        get() = FragmentForoomContainerHomeBinding::inflate
    override val viewModel: ForoomHomeContainerViewModel by viewModel()

    private val chatsFragment: ForoomHomeChatsFragment
        get() = childFragmentManager.findFragmentByTag(CHATS_TAG) as? ForoomHomeChatsFragment
            ?: ForoomHomeChatsFragment()
    private val profileFragment: ForoomProfileFragment
        get() = childFragmentManager.findFragmentByTag(PROFILE_TAG) as? ForoomProfileFragment
            ?: ForoomProfileFragment()
    private var selectedPage = PAGE_INDEX_CHATS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedPage = savedInstanceState?.getInt(SELECTED_PAGE_KEY)
            ?: navArgs?.index ?: PAGE_INDEX_CHATS
        setUpNavigation()
        setObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_PAGE_KEY, selectedPage)
    }

    private fun setUpNavigation() = with(binding.navBar) {
        onNavigationButtonClick = { button ->

            when (button.id) {
                R.id.homeNavigationChats -> {
                    selectedPage = PAGE_INDEX_CHATS
                    addOrShowPage(chatsFragment, CHATS_TAG)
                }

                R.id.homeNavigationCreateChat -> {
                    navigationHost?.openNextPage(ForoomCreateChatFragment(), animate = false)
                }

                R.id.homeNavigationProfile -> {
                    selectedPage = PAGE_INDEX_PROFILE
                    addOrShowPage(profileFragment, PROFILE_TAG)
                }
            }

        }

        selectAt(selectedPage)
    }

    private fun setObservers() {
        eventsHub?.observeEvent<ForoomHomeEvents.HomeNavigationEvents>(viewLifecycleOwner) { event ->
            when (event.type) {
                HomeNavigationType.CHATS -> {
                    binding.homeNavigationChats.performClick()
                }

                HomeNavigationType.PROFILE -> {
                    binding.homeNavigationProfile.performClick()
                }
            }
        }
    }

    private fun addOrShowPage(fragment: BaseFragment<*, *>, tag: String) {
        val transaction = childFragmentManager.beginTransaction()

        childFragmentManager.fragments.forEach(transaction::hide)
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(
                R.id.homeContainer,
                fragment,
                tag
            )
        }

        transaction.commitNow()
    }

    companion object {
        private const val CHATS_TAG = "homeChats"
        private const val PROFILE_TAG = "homeProfile"
        private const val SELECTED_PAGE_KEY = "selectedPage"
        const val PAGE_INDEX_CHATS = 0
        const val PAGE_INDEX_PROFILE = 2
    }
}

@Parcelize
data class ForoomHomeContainerInitialScreenIndex(val index: Int): Parcelable
