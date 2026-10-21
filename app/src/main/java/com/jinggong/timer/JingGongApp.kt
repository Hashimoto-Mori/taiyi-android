package com.jinggong.timer

import android.app.Application
import com.jinggong.timer.di.AppContainer

class JingGongApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.initialize()
    }
}
