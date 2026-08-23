plugins {
    alias(libs.plugins.apkanalyzer.baselineprofile)
}

baselineProfile {
    useConnectedDevices = false
    managedDevices += providers.gradleProperty("baselineProfileManagedDeviceName").get()
}

android {
    namespace = "sk.styk.martin.apkanalyzer.baselineprofile"
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
