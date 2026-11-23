package com.xplayer.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.datastore.AppearancePrefs
import com.xplayer.app.data.datastore.DarkModeTheme
import com.xplayer.app.data.datastore.EnginePolicy
import com.xplayer.app.data.datastore.EnginePrefs
import com.xplayer.app.data.datastore.GesturePrefs
import com.xplayer.app.data.datastore.MpvHwdecMode
import com.xplayer.app.data.datastore.PlaybackPrefs
import com.xplayer.app.data.datastore.SubtitlePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val repo = app.preferencesRepository
    private val scanner = app.mediaScanner

    val appearancePrefs: StateFlow<AppearancePrefs> = repo.appearancePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearancePrefs())

    val enginePrefs: StateFlow<EnginePrefs> = repo.enginePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EnginePrefs())

    val gesturePrefs: StateFlow<GesturePrefs> = repo.gesturePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GesturePrefs())

    val subtitlePrefs: StateFlow<SubtitlePrefs> = repo.subtitlePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubtitlePrefs())

    val playbackPrefs: StateFlow<PlaybackPrefs> = repo.playbackPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackPrefs())

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repo.updateAppearance { it.copy(dynamicColor = enabled) } }
    }

    fun updateDarkMode(mode: DarkModeTheme) {
        viewModelScope.launch { repo.updateAppearance { it.copy(darkMode = mode) } }
    }

    fun updateSeedColorIndex(index: Int) {
        viewModelScope.launch { repo.updateAppearance { it.copy(seedColorIndex = index) } }
    }

    fun updateEnginePolicy(policy: EnginePolicy) {
        viewModelScope.launch { repo.updateEngine { it.copy(enginePolicy = policy) } }
    }

    fun updateAutoSwitchAss(enabled: Boolean) {
        viewModelScope.launch { repo.updateEngine { it.copy(autoSwitchAss = enabled) } }
    }

    fun updateFrameRateMatching(enabled: Boolean) {
        viewModelScope.launch { repo.updateEngine { it.copy(frameRateMatching = enabled) } }
    }

    fun updateMpvHwdec(mode: MpvHwdecMode) {
        viewModelScope.launch { repo.updateEngine { it.copy(mpvHwdec = mode) } }
    }

    fun updateCustomMpvConf(enabled: Boolean, conf: String) {
        viewModelScope.launch { repo.updateEngine { it.copy(customMpvConfEnabled = enabled, customMpvConf = conf) } }
    }

    fun updateGestures(update: (GesturePrefs) -> GesturePrefs) {
        viewModelScope.launch { repo.updateGestures(update) }
    }

    fun resetGestures() {
        viewModelScope.launch { repo.resetGesturesToDefault() }
    }

    fun updateSubtitles(update: (SubtitlePrefs) -> SubtitlePrefs) {
        viewModelScope.launch { repo.updateSubtitles(update) }
    }

    fun updatePlayback(update: (PlaybackPrefs) -> PlaybackPrefs) {
        viewModelScope.launch { repo.updatePlayback(update) }
    }

    fun triggerRescan() {
        viewModelScope.launch { scanner.scanMedia() }
    }
}
