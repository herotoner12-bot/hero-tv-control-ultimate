// مكتبة libadb-android (MuntashirAkon) v3.1.1 منسوخة كمصدر.
// الترخيص: GPL-3.0-or-later أو Apache-2.0 (انظر COPYING و LICENSES/). نستخدمها بموجب Apache-2.0.
plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.muntashirakon.adb"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.81")
    implementation("com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1")
}
