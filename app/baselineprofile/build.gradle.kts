plugins {
    alias(libs.plugins.apkanalyzer.baselineprofile)
}

baselineProfile {
    useConnectedDevices = false
    managedDevices += "pixel6ApiTarget"
}

android {
    namespace = "sk.styk.martin.apkanalyzer.baselineprofile"
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
