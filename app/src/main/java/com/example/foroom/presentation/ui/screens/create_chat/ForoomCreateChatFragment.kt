package com.example.foroom.presentation.ui.screens.create_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.alternator.foroom.R
import com.alternator.foroom.databinding.FragmentForoomCreateChatBinding
import com.example.foroom.presentation.ui.screens.chat.ForoomChatFragment
import com.example.foroom.presentation.ui.screens.home.chats.events.ForoomHomeChatsEvents
import com.example.navigation.host.openNextPage
import com.example.navigation.util.navigationHost
import com.example.shared.extension.handleResult
import com.example.shared.extension.onClick
import com.example.shared.model.Image
import com.example.shared.ui.fragment.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForoomCreateChatFragment :
    BaseFragment<ForoomCreateChatViewModel, FragmentForoomCreateChatBinding>() {
    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> FragmentForoomCreateChatBinding
        get() = FragmentForoomCreateChatBinding::inflate
    override val viewModel: ForoomCreateChatViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setObservers()
        setListeners()
    }

    private fun initViews() {
        binding.chatHeaderView.setChatTitle(getString(R.string.create_chat_chat_name))
    }

    private fun setObservers() {
        viewModel.emojisLiveData.handleResult(viewLifecycleOwner) {
            onSuccess { images ->
                binding.chatImageChooser.images = images
            }

            onLoading {
                binding.chatImageChooser.images = Image.getBlankImages(BLANK_IMAGE_COUNT)
            }
        }

        viewModel.currentUserNameLiveData.observe(viewLifecycleOwner) { userName ->
            binding.chatHeaderView.setAuthorName(userName)
        }

        viewModel.createChatLiveData.handleResult(viewLifecycleOwner) {
            onLoading {
                globalLoadingDelegate?.showGlobalLoading()
            }

            onError {
                globalLoadingDelegate?.hideGlobalLoading()
            }

            onSuccess { chat ->
                globalLoadingDelegate?.hideGlobalLoading()
                eventsHub?.sendEvent(ForoomHomeChatsEvents.RefreshChats)
                navigationHost?.let { host ->
                    host.getHostFragmentManager().popBackStackImmediate()
                    host.openNextPage(ForoomChatFragment(), args = chat, addToBackStack = true, animate = false)
                }
            }
        }
    }

    private fun setListeners() {
        binding.chatImageChooser.onImageChooseCallback = { image ->
            binding.chatHeaderView.setChatImageUrl(image.url)
        }

        binding.chatNameInput.editText.addTextChangedListener { title ->
            binding.chatHeaderView.setChatTitle(
                if (title.isNullOrBlank()) getString(R.string.create_chat_chat_name)
                else title.toString()
            )
        }

        binding.closeButton.onClick {
            navigationHost?.goBack()
        }

        binding.createChatButton.onClick {
            binding.chatImageChooser.selectedImage?.id?.let { emojiId ->
                viewModel.createChat(
                    binding.chatNameInput.text,
                    emojiId
                )
            }
        }
    }

    companion object {
        private const val BLANK_IMAGE_COUNT = 9
    }
}