package com.example.foroom.presentation.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.alternator.foroom.R
import com.alternator.foroom.databinding.ActivityForoomBinding
import com.example.foroom.presentation.ui.screens.home.container.ForoomHomeContainerFragment
import com.example.foroom.presentation.ui.screens.home.container.ForoomHomeContainerInitialScreenIndex
import com.example.foroom.presentation.ui.screens.log_in.ForoomLoginFragment
import com.example.foroom.presentation.ui.screens.registration.ForoomRegistrationFragment
import com.example.foroom.presentation.ui.util.datastore.user.ForoomUserDataStore
import com.example.navigation.host.ForoomNavigationHost
import com.example.navigation.host.hasBackStack
import com.example.navigation.host.openNextPage
import com.example.navigation.host.openRootPage
import com.example.navigation.host.popBackStack
import com.example.shared.extension.INVALID_VALUE
import com.example.shared.extension.handleResult
import com.example.shared.extension.isLoading
import com.example.shared.model.ForoomLanguage
import com.example.shared.ui.activity.BaseActivity
import com.example.shared.util.events.ForoomEventsHub
import com.example.shared.util.events.ForoomEventsHubHolder
import com.example.shared.util.language_change.AppLanguageDelegate
import com.example.shared.util.language_change.LanguageChangePoint
import com.example.shared.util.loading.GlobalLoadingDelegate
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForoomActivity : ForoomNavigationHost, GlobalLoadingDelegate, ForoomEventsHubHolder,
    AppLanguageDelegate, BaseActivity<ActivityForoomBinding>(ActivityForoomBinding::inflate) {
    override val fragmentContainerId: Int = R.id.fragmentContainerView
    override val eventsHub: ForoomEventsHub = get()

    private val viewModel by viewModel<ForoomActivityViewModel>()
    private val userDataStore = get<ForoomUserDataStore>()
    private var languageChangeRecreatePoint: LanguageChangePoint? = null

    override fun getHostFragmentManager(): FragmentManager {
        return supportFragmentManager
    }

    override fun goBack() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun attachBaseContext(base: Context?) {
        val savedLocale = runBlocking { userDataStore.getUserLanguage() }

        super.attachBaseContext(
            wrapContext(
                base, savedLocale ?: ForoomLanguage.KA.langName
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { savedState ->
            languageChangeRecreatePoint = LanguageChangePoint.fromValue(
                savedState.getInt(APP_LANGUAGE_CHANGE_POINT_KEY, INVALID_VALUE)
            )
        }

        // prevent background clicks
        binding.loadingViewBackground.setOnClickListener {}
        viewModel.refreshSession()
        setObservers()
    }

    override fun showGlobalLoading() {
        globalLoadingVisible(true)
    }

    override fun hideGlobalLoading() {
        globalLoadingVisible(false)
    }

    override fun changeAppLanguage(language: ForoomLanguage, changePoint: LanguageChangePoint) {
        lifecycleScope.launch {
            viewModel.updateUserLanguage(language)

            languageChangeRecreatePoint = changePoint
            recreate()
        }
    }

    override suspend fun getAppLanguage(): ForoomLanguage? {
        return viewModel.getUserLanguage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        languageChangeRecreatePoint?.let { point ->
            outState.putInt(APP_LANGUAGE_CHANGE_POINT_KEY, point.value)

            languageChangeRecreatePoint = null
        }
    }

    private fun setObservers() {
        viewModel.currentUserLiveData.handleResult(this) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { true }

            onSuccess {
                if (languageChangeRecreatePoint != null) restoreSavedPoint()
                else if (supportFragmentManager.findFragmentById(fragmentContainerId) == null) {
                    openRootPage(ForoomHomeContainerFragment())
                }
            }

            onError {
                if (languageChangeRecreatePoint == LanguageChangePoint.REGISTRATION ||
                    languageChangeRecreatePoint == LanguageChangePoint.LOG_IN) {
                    restoreSavedPoint()
                } else {
                    languageChangeRecreatePoint = null
                    openRootPage(ForoomLoginFragment())
                }
            }

            onResult { result ->
                if (!result.isLoading) splashScreen.setKeepOnScreenCondition { false }
            }
        }
    }

    private fun restoreSavedPoint() {
        val targetPage = when (languageChangeRecreatePoint) {
            LanguageChangePoint.LOG_IN, null -> ForoomLoginFragment()
            LanguageChangePoint.REGISTRATION -> ForoomRegistrationFragment()
            LanguageChangePoint.POFILE -> ForoomHomeContainerFragment()
        }

        languageChangeRecreatePoint = null
        val previousScreensAvailable = hasBackStack()
        popBackStack()
        openNextPage(
            targetPage,
            addToBackStack = previousScreensAvailable,
            animate = false,
            args = ForoomHomeContainerInitialScreenIndex(ForoomHomeContainerFragment.PAGE_INDEX_PROFILE)
        )
    }

    private fun globalLoadingVisible(visible: Boolean) {
        binding.loadingViewBackground.isVisible = visible

        with(binding.loadingLottie) {
            isVisible = visible
            if (visible) playAnimation()
            else cancelAnimation()
        }
    }

    companion object {
        private const val APP_LANGUAGE_CHANGE_POINT_KEY = "appLanguageChangePoint"
    }
}
