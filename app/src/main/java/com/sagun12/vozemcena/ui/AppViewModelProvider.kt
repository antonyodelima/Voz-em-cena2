package com.sagun12.vozemcena.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sagun12.vozemcena.VozEmCenaApplication
import com.sagun12.vozemcena.ui.admin.AdminViewModel
import com.sagun12.vozemcena.ui.dubbing.DubbingViewModel
import com.sagun12.vozemcena.ui.home.HomeViewModel
import com.sagun12.vozemcena.ui.library.LibraryViewModel
import com.sagun12.vozemcena.ui.voices.VoicesViewModel
import com.sagun12.vozemcena.ui.voices.clone.VoiceCloneViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = vozemcenaApplication()
            HomeViewModel(
                context = app.applicationContext,
                projectRepository = app.projectRepository,
                voiceRepository = app.voiceRepository
            )
        }
        initializer {
            val app = vozemcenaApplication()
            VoicesViewModel(
                context = app.applicationContext,
                voiceRepository = app.voiceRepository
            )
        }
        initializer {
            val app = vozemcenaApplication()
            LibraryViewModel(
                context = app.applicationContext,
                projectRepository = app.projectRepository
            )
        }
        initializer {
            val app = vozemcenaApplication()
            DubbingViewModel(
                context = app.applicationContext,
                voiceRepository = app.voiceRepository,
                cartesiaRetrofitService = app.cartesiaModule.retrofitService,
                settingsStore = app.settingsStore,
                googleAiRepository = app.googleAiRepository
            )
        }
        initializer {
            val app = vozemcenaApplication()
            VoiceCloneViewModel(
                context = app.applicationContext,
                voiceRepository = app.voiceRepository,
                cartesiaRetrofitService = app.cartesiaModule.retrofitService,
                settingsStore = app.settingsStore
            )
        }
        initializer {
            val app = vozemcenaApplication()
            AdminViewModel(
                context = app.applicationContext,
                settingsStore = app.settingsStore,
                cartesiaApi = app.cartesiaModule.api,
                cronJobRepository = app.cronJobRepository
            )
        }
    }
}

fun CreationExtras.vozemcenaApplication(): VozEmCenaApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as VozEmCenaApplication)
