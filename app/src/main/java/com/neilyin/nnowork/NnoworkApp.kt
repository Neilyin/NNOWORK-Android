package com.neilyin.nnowork

import android.app.Application
import com.neilyin.nnowork.di.ServiceLocator

class NnoworkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
