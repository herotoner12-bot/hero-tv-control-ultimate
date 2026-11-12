package com.herotoner.herotv

import android.app.Application
import com.herotoner.herotv.adb.TvRepository
import com.herotoner.herotv.pad.ProfilesStore
import com.herotoner.herotv.settings.SettingsStore

class HeroTvApp : Application() {
    val tv: TvRepository by lazy { TvRepository(this) }
    val pad: ProfilesStore by lazy { ProfilesStore(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        // نبدأ المستودع مبكراً كي تبدأ إعادة الاتصال التلقائية بآخر تلفزيون
        tv
    }
}
