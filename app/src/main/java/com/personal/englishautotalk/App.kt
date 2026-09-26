package com.personal.englishautotalk

import android.app.Application
import com.personal.englishautotalk.talk.TalkNotifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TalkNotifier.createChannel(this)
    }
}
