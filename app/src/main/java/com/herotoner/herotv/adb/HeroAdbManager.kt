package com.herotoner.herotv.adb

import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.security.PrivateKey
import java.security.cert.Certificate

class HeroAdbManager(private val keys: AdbKeys) : AbsAdbConnectionManager() {
    override fun getPrivateKey(): PrivateKey = keys.privateKey

    override fun getCertificate(): Certificate = keys.certificate

    override fun getDeviceName(): String = "HERO-TV-CONTROL"
}
