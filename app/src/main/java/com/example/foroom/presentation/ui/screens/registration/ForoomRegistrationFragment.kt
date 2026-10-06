package com.example.foroom.presentation.ui.screens.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.design_system.R
import com.example.design_system.components.input.Input
import com.example.design_system.components.shimmer.ForoomShimmerDrawableBuilder
import com.alternator.foroom.databinding.FragmentForoomRegistrationBinding
import com.example.foroom.presentation.ui.screens.home.container.ForoomHomeContainerFragment
import com.example.foroom.presentation.ui.util.validator.BlankInputValidation
import com.example.navigation.host.openRootPage
import com.example.navigation.util.navigationHost
import com.example.network.ifHttpError
import com.example.network.model.response.AuthenticationError
import com.example.shared.extension.handleResult
import com.example.shared.extension.ifNot
import com.example.shared.extension.isLoading
import com.example.shared.extension.loadImageUrl
import com.example.shared.extension.makeTextClickable
import com.example.shared.extension.onClick
import com.example.shared.extension.orEmpty
import com.example.shared.extension.toast
import com.example.shared.model.ForoomLanguage
import com.example.shared.model.Image
import com.example.shared.ui.fragment.BaseFragment
import com.example.shared.util.language_change.LanguageChangePoint
import com.example.shared.util.loading.isLoading
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForoomRegistrationFragment :
    BaseFragment<ForoomRegistrationViewModel, FragmentForoomRegistrationBinding>() {
    override val inflate: (LayoutInflater, ViewGroup?, Boolean) -> FragmentForoomRegistrationBinding
        get() = FragmentForoomRegistrationBinding::inflate
    override val viewModel: ForoomRegistrationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setListeners()
        setObservers()
    }

    private fun initViews() {
        initInputs()

        binding.logInTextView.makeTextClickable(requireContext().getColor(R.color.foroom_main_green)) {
            navigationHost?.goBack()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val language = appLanguageDelegate?.getAppLanguage() ?: ForoomLanguage.KA
                binding.languageSelector.selectLanguage(language)
                cancel()
            }
        }
    }

    private fun setListeners() {
        binding.listView.onImageChooseCallback = { image ->
            binding.imageView.image.loadImageUrl(
                image.url,
                ForoomShimmerDrawableBuilder.getDefaultDrawable(requireContext())
            )
            viewModel.avatarId = image.id
        }

        binding.signUpButton.onClick {
            val isUsernameValid = binding.userNameInput.validate()
            val isPasswordValid = binding.passwordInput.validate()
            val isRepeatPasswordValid = binding.repeatPasswordInput.validate()

            if (isUsernameValid && isPasswordValid && isRepeatPasswordValid) viewModel.register()
        }

        binding.languageSelector.onLanguageSelected = { language ->
            appLanguageDelegate?.changeAppLanguage(language, LanguageChangePoint.REGISTRATION)
        }
    }

    private fun setObservers() {
        viewModel.avatarsLiveData.handleResult(viewLifecycleOwner) {
            with(binding.listView) {
                onSuccess { data ->
                    images = data
                    isChoosingEnabled = true
                }

                onError { error ->
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                }

                onLoading {
                    images = Image.getBlankImages(LOADING_IMAGE_COUNT)
                    isChoosingEnabled = false
                }
            }
        }

        viewModel.getAndSaveUserResultLiveData.handleResult(viewLifecycleOwner) {
            onSuccess {
                navigationHost?.openRootPage(ForoomHomeContainerFragment())
            }
        }

        viewModel.registrationLiveData.handleResult(viewLifecycleOwner) {

            onResult { result ->
                globalLoadingDelegate?.isLoading(result.isLoading)
            }

            onError { exception ->
                exception.ifHttpError<AuthenticationError> { _, error ->
                    error.usernameError?.let { message ->
                        binding.userNameInput.setInputDescription(
                            message,
                            Input.DescriptionType.ERROR,
                            true
                        )
                    }

                    error.passwordError?.let { message ->
                        binding.passwordInput.setInputDescription(
                            message,
                            Input.DescriptionType.ERROR,
                            true
                        )
                    }
                }.ifNot {
                    context?.toast(exception.message)
                }
            }
        }
    }

    private fun initInputs() {
        with(binding.userNameInput) {
            addValidation(BlankInputValidation)
            editText.addTextChangedListener { text ->
                viewModel.userName = text?.toString().orEmpty()
            }
        }

        with(binding.passwordInput) {
            addValidation(BlankInputValidation)
            editText.addTextChangedListener { text ->
                viewModel.password = text?.toString().orEmpty()
            }
        }

        with(binding.repeatPasswordInput) {
            addValidation(BlankInputValidation)
            editText.addTextChangedListener { text ->
                viewModel.repeatedPassword = text?.toString().orEmpty()
            }
        }
    }

    companion object {
        private const val LOADING_IMAGE_COUNT = 6
    }
}
