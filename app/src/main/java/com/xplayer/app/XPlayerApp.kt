package com.xplayer.app

import android.app.Application
import com.xplayer.app.data.datastore.UserPreferencesRepository
import com.xplayer.app.data.local.AppDatabase
import com.xplayer.app.data.scanner.MediaScanner

class XPlayerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var mediaScanner: MediaScanner
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        mediaScanner = MediaScanner(this)
    }

    companion object {
        lateinit var instance: XPlayerApp
            private set
    }
}
