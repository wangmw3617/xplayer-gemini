package com.xplayer.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.local.entity.MpvShaderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MpvLabViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val shaderDao = app.database.mpvShaderDao()
    private val prefsRepo = app.preferencesRepository

    val shaders: StateFlow<List<MpvShaderEntity>> = shaderDao.getAllShadersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enginePrefs = prefsRepo.enginePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.xplayer.app.data.datastore.EnginePrefs())

    private val _confText = MutableStateFlow("")
    val confText: StateFlow<String> = _confText.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.enginePrefsFlow.collect { prefs ->
                _confText.value = prefs.customMpvConf
            }
        }
    }

    fun updateConfText(text: String) {
        _confText.value = text
    }

    fun saveConf(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEngine {
                it.copy(
                    customMpvConfEnabled = enabled,
                    customMpvConf = _confText.value
                )
            }
        }
    }

    fun resetConf() {
        val defaultConf = "# mpv.conf\nvo=gpu-next\ngpu-context=android\nhwdec=mediacodec\n"
        _confText.value = defaultConf
        saveConf(false)
    }

    fun insertPreset(preset: String) {
        _confText.value = _confText.value.trimEnd() + "\n" + preset + "\n"
    }

    fun toggleShader(shader: MpvShaderEntity) {
        viewModelScope.launch {
            shaderDao.updateShader(shader.copy(enabled = !shader.enabled))
        }
    }

    fun deleteShader(shader: MpvShaderEntity) {
        viewModelScope.launch {
            runCatching { File(shader.filePath).delete() }
            shaderDao.deleteById(shader.id)
        }
    }

    fun importShader(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = app.contentResolver
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "shader_${System.currentTimeMillis()}.glsl"
            val shaderDir = File(app.filesDir, "shaders")
            if (!shaderDir.exists()) shaderDir.mkdirs()

            val targetFile = File(shaderDir, fileName)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val shader = MpvShaderEntity(
                name = fileName.removeSuffix(".glsl"),
                filePath = targetFile.absolutePath,
                enabled = true,
                orderIndex = 0
            )
            shaderDao.insertShader(shader)
        }
    }
}
