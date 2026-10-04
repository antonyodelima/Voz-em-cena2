package com.sagun12.vozemcena

import android.app.Application
import com.sagun12.vozemcena.data.local.AppDatabase
import com.sagun12.vozemcena.data.remote.CartesiaModule
import com.sagun12.vozemcena.data.remote.CronJobModule
import com.sagun12.vozemcena.data.remote.GoogleAiModule
import com.sagun12.vozemcena.data.repository.CronJobRepository
import com.sagun12.vozemcena.data.repository.GoogleAiRepository
import com.sagun12.vozemcena.data.repository.ProjectRepository
import com.sagun12.vozemcena.data.repository.VoiceRepository
import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VozEmCenaApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val settingsStore by lazy { CartesiaSettingsStore(this) }
    val cartesiaModule by lazy { CartesiaModule(settingsStore) }
    val googleAiModule by lazy { GoogleAiModule(settingsStore) }
    val cronJobModule by lazy { CronJobModule(settingsStore) }

    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val googleAiRepository by lazy { GoogleAiRepository(googleAiModule.client) }
    val cronJobRepository by lazy { CronJobRepository(cronJobModule.client) }
    val voiceRepository by lazy {
        VoiceRepository(
            context = this,
            voiceDao = database.voiceDao(),
            cartesiaApi = cartesiaModule.api,
            settingsStore = settingsStore
        )
    }
}
