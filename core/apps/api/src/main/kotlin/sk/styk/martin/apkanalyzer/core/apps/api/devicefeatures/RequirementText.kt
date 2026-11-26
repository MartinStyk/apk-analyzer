package sk.styk.martin.apkanalyzer.core.apps.devicefeatures

import android.content.res.Resources
import androidx.annotation.StringRes
import sk.styk.martin.apkanalyzer.core.apps.R

@get:StringRes
val RequirementItem.Hardware.labelRes: Int?
    get() = requirementLabels[name]

fun requirementVersionName(name: String, version: Int): String = when (name) {
    VULKAN_VERSION_FEATURE -> "${version ushr 22}.${(version ushr 12) and 0x3FF}"
    else -> version.toString()
}

private const val VULKAN_VERSION_FEATURE = "android.hardware.vulkan.version"

@get:StringRes
val FeatureAvailability.labelRes: Int
    get() = when (this) {
        FeatureAvailability.Available -> R.string.requirements_availability_available
        FeatureAvailability.Missing -> R.string.requirements_availability_missing
        FeatureAvailability.Unknown -> R.string.requirements_availability_unknown
    }

@StringRes
fun RequirementItem.availabilityExplanationRes(): Int = when (availability) {
    FeatureAvailability.Available -> R.string.requirements_availability_available_explanation

    FeatureAvailability.Missing -> if (isRequired) {
        R.string.requirements_availability_missing_required_explanation
    } else {
        R.string.requirements_availability_missing_optional_explanation
    }

    FeatureAvailability.Unknown -> R.string.requirements_availability_unknown_explanation
}

@get:StringRes
val RequirementSection.labelRes: Int
    get() = if (isRequired) R.string.requirements_section_required else R.string.requirements_section_optional

@get:StringRes
val RequirementSection.explanationRes: Int
    get() = if (isRequired) R.string.requirements_section_required_explanation else R.string.requirements_section_optional_explanation

private val requirementLabels = mapOf(
    "android.hardware.camera" to R.string.requirement_camera,
    "android.hardware.camera.any" to R.string.requirement_camera_any,
    "android.hardware.camera.front" to R.string.requirement_camera_front,
    "android.hardware.camera.flash" to R.string.requirement_camera_flash,
    "android.hardware.camera.autofocus" to R.string.requirement_camera_autofocus,
    "android.hardware.camera.external" to R.string.requirement_camera_external,
    "android.hardware.camera.level.full" to R.string.requirement_camera_level_full,
    "android.hardware.camera.capability.raw" to R.string.requirement_camera_raw,
    "android.hardware.microphone" to R.string.requirement_microphone,
    "android.hardware.bluetooth" to R.string.requirement_bluetooth,
    "android.hardware.bluetooth_le" to R.string.requirement_bluetooth_le,
    "android.hardware.wifi" to R.string.requirement_wifi,
    "android.hardware.wifi.direct" to R.string.requirement_wifi_direct,
    "android.hardware.wifi.aware" to R.string.requirement_wifi_aware,
    "android.hardware.nfc" to R.string.requirement_nfc,
    "android.hardware.nfc.hce" to R.string.requirement_nfc_hce,
    "android.hardware.location" to R.string.requirement_location,
    "android.hardware.location.gps" to R.string.requirement_location_gps,
    "android.hardware.location.network" to R.string.requirement_location_network,
    "android.hardware.telephony" to R.string.requirement_telephony,
    "android.hardware.telephony.gsm" to R.string.requirement_telephony_gsm,
    "android.hardware.telephony.cdma" to R.string.requirement_telephony_cdma,
    "android.hardware.touchscreen" to R.string.requirement_touchscreen,
    "android.hardware.touchscreen.multitouch" to R.string.requirement_touchscreen_multitouch,
    "android.hardware.touchscreen.multitouch.distinct" to R.string.requirement_touchscreen_multitouch_distinct,
    "android.hardware.touchscreen.multitouch.jazzhand" to R.string.requirement_touchscreen_multitouch_jazzhand,
    "android.hardware.faketouch" to R.string.requirement_faketouch,
    "android.hardware.screen.portrait" to R.string.requirement_screen_portrait,
    "android.hardware.screen.landscape" to R.string.requirement_screen_landscape,
    "android.hardware.sensor.accelerometer" to R.string.requirement_sensor_accelerometer,
    "android.hardware.sensor.gyroscope" to R.string.requirement_sensor_gyroscope,
    "android.hardware.sensor.compass" to R.string.requirement_sensor_compass,
    "android.hardware.sensor.light" to R.string.requirement_sensor_light,
    "android.hardware.sensor.proximity" to R.string.requirement_sensor_proximity,
    "android.hardware.sensor.barometer" to R.string.requirement_sensor_barometer,
    "android.hardware.sensor.stepcounter" to R.string.requirement_sensor_step_counter,
    "android.hardware.sensor.stepdetector" to R.string.requirement_sensor_step_detector,
    "android.hardware.sensor.heartrate" to R.string.requirement_sensor_heart_rate,
    "android.hardware.fingerprint" to R.string.requirement_fingerprint,
    "android.hardware.biometrics.face" to R.string.requirement_face_unlock,
    "android.hardware.usb.host" to R.string.requirement_usb_host,
    "android.hardware.usb.accessory" to R.string.requirement_usb_accessory,
    "android.hardware.audio.output" to R.string.requirement_audio_output,
    "android.hardware.audio.low_latency" to R.string.requirement_audio_low_latency,
    "android.hardware.audio.pro" to R.string.requirement_audio_pro,
    "android.hardware.gamepad" to R.string.requirement_gamepad,
    "android.hardware.vulkan.level" to R.string.requirement_vulkan,
    "android.hardware.vulkan.version" to R.string.requirement_vulkan,
    "android.hardware.opengles.aep" to R.string.requirement_opengles_aep,
    "android.hardware.consumerir" to R.string.requirement_infrared,
    "android.hardware.ram.low" to R.string.requirement_ram_low,
    "android.hardware.ram.normal" to R.string.requirement_ram_normal,
    "android.hardware.type.watch" to R.string.requirement_type_watch,
    "android.hardware.type.television" to R.string.requirement_type_television,
    "android.hardware.type.automotive" to R.string.requirement_type_automotive,
    "android.hardware.type.pc" to R.string.requirement_type_pc,
    "android.hardware.vr.high_performance" to R.string.requirement_vr,
    "android.hardware.vr.headtracking" to R.string.requirement_vr_headtracking,
    "android.software.vr.mode" to R.string.requirement_vr_mode,
    "android.software.webview" to R.string.requirement_webview,
    "android.software.print" to R.string.requirement_print,
    "android.software.midi" to R.string.requirement_midi,
    "android.software.leanback" to R.string.requirement_leanback,
    "android.software.live_wallpaper" to R.string.requirement_live_wallpaper,
    "android.software.app_widgets" to R.string.requirement_app_widgets,
    "android.software.home_screen" to R.string.requirement_home_screen,
    "android.software.input_methods" to R.string.requirement_input_methods,
    "android.software.device_admin" to R.string.requirement_device_admin,
    "android.software.managed_users" to R.string.requirement_managed_users,
    "android.software.verified_boot" to R.string.requirement_verified_boot,
    "android.software.picture_in_picture" to R.string.requirement_picture_in_picture,
    "android.software.companion_device_setup" to R.string.requirement_companion_device,
    "android.software.autofill" to R.string.requirement_autofill,
    "android.software.backup" to R.string.requirement_backup,
    "android.software.sip.voip" to R.string.requirement_sip_voip,
    "android.software.connectionservice" to R.string.requirement_connection_service,
)

fun RequirementItem.label(resources: Resources): String = when (this) {
    is RequirementItem.Hardware -> {
        val displayName = labelRes?.let { resources.getString(it) } ?: name
        if (requiredVersion == Feature.VERSION_UNSPECIFIED) {
            displayName
        } else {
            resources.getString(R.string.requirements_label_with_version, displayName, requirementVersionName(identifier, requiredVersion))
        }
    }

    is RequirementItem.OpenGlEs -> resources.getString(R.string.requirements_open_gl_es, versionName)
}

fun RequirementItem.missingText(resources: Resources): String {
    val deviceGlEsVersionName = (this as? RequirementItem.OpenGlEs)?.deviceVersionName
    val deviceHardwareVersion = (this as? RequirementItem.Hardware)?.takeIf { it.isVersionMiss }?.deviceVersion
    return when {
        this is RequirementItem.OpenGlEs && deviceGlEsVersionName != null ->
            resources.getString(R.string.requirements_missing_version, versionName, deviceGlEsVersionName)

        this is RequirementItem.Hardware && deviceHardwareVersion != null -> resources.getString(
            R.string.requirements_missing_version,
            requirementVersionName(name, requiredVersion),
            requirementVersionName(name, deviceHardwareVersion),
        )

        else -> resources.getString(R.string.requirements_missing)
    }
}
