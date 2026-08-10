package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.FeatureAvailability
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.R

internal val RequirementItem.icon: ImageVector
    get() = when (this) {
        is RequirementItem.Hardware -> requirementIcon(name)
        is RequirementItem.OpenGlEs -> ApkAnalyzerIcons.Graphics
    }

internal fun requirementIcon(name: String?): ImageVector = when (name) {
    null -> ApkAnalyzerIcons.Graphics
    else -> requirementIcons[name] ?: ApkAnalyzerIcons.Android
}

@get:StringRes
internal val RequirementItem.Hardware.labelRes: Int?
    get() = requirementLabels[name]

internal fun requirementVersionName(name: String, version: Int): String = when (name) {
    VULKAN_VERSION_FEATURE -> "${version ushr 22}.${(version ushr 12) and 0x3FF}"
    else -> version.toString()
}

private const val VULKAN_VERSION_FEATURE = "android.hardware.vulkan.version"

@get:StringRes
internal val FeatureAvailability.labelRes: Int
    get() = when (this) {
        FeatureAvailability.Available -> R.string.requirements_availability_available
        FeatureAvailability.Missing -> R.string.requirements_availability_missing
        FeatureAvailability.Unknown -> R.string.requirements_availability_unknown
    }

@StringRes
internal fun RequirementItem.availabilityExplanationRes(): Int = when (availability) {
    FeatureAvailability.Available -> R.string.requirements_availability_available_explanation

    FeatureAvailability.Missing -> if (isRequired) {
        R.string.requirements_availability_missing_required_explanation
    } else {
        R.string.requirements_availability_missing_optional_explanation
    }

    FeatureAvailability.Unknown -> R.string.requirements_availability_unknown_explanation
}

@get:StringRes
internal val RequirementSection.labelRes: Int
    get() = if (isRequired) R.string.requirements_section_required else R.string.requirements_section_optional

@get:StringRes
internal val RequirementSection.explanationRes: Int
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

private val requirementIcons = mapOf(
    "android.hardware.camera" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.any" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.front" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.flash" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.autofocus" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.external" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.level.full" to ApkAnalyzerIcons.Camera,
    "android.hardware.camera.capability.raw" to ApkAnalyzerIcons.Camera,
    "android.hardware.microphone" to ApkAnalyzerIcons.Microphone,
    "android.hardware.bluetooth" to ApkAnalyzerIcons.Bluetooth,
    "android.hardware.bluetooth_le" to ApkAnalyzerIcons.Bluetooth,
    "android.hardware.wifi" to ApkAnalyzerIcons.Wifi,
    "android.hardware.wifi.direct" to ApkAnalyzerIcons.Wifi,
    "android.hardware.wifi.aware" to ApkAnalyzerIcons.Wifi,
    "android.hardware.nfc" to ApkAnalyzerIcons.Nfc,
    "android.hardware.nfc.hce" to ApkAnalyzerIcons.Nfc,
    "android.hardware.location" to ApkAnalyzerIcons.Location,
    "android.hardware.location.gps" to ApkAnalyzerIcons.Location,
    "android.hardware.location.network" to ApkAnalyzerIcons.Location,
    "android.hardware.telephony" to ApkAnalyzerIcons.Phone,
    "android.hardware.telephony.gsm" to ApkAnalyzerIcons.Phone,
    "android.hardware.telephony.cdma" to ApkAnalyzerIcons.Phone,
    "android.hardware.touchscreen" to ApkAnalyzerIcons.Touch,
    "android.hardware.touchscreen.multitouch" to ApkAnalyzerIcons.Touch,
    "android.hardware.touchscreen.multitouch.distinct" to ApkAnalyzerIcons.Touch,
    "android.hardware.touchscreen.multitouch.jazzhand" to ApkAnalyzerIcons.Touch,
    "android.hardware.faketouch" to ApkAnalyzerIcons.Touch,
    "android.hardware.screen.portrait" to ApkAnalyzerIcons.Screen,
    "android.hardware.screen.landscape" to ApkAnalyzerIcons.Screen,
    "android.hardware.sensor.accelerometer" to ApkAnalyzerIcons.Sensors,
    "android.hardware.sensor.gyroscope" to ApkAnalyzerIcons.Sensors,
    "android.hardware.sensor.compass" to ApkAnalyzerIcons.Compass,
    "android.hardware.sensor.light" to ApkAnalyzerIcons.Sensors,
    "android.hardware.sensor.proximity" to ApkAnalyzerIcons.Sensors,
    "android.hardware.sensor.barometer" to ApkAnalyzerIcons.Sensors,
    "android.hardware.sensor.stepcounter" to ApkAnalyzerIcons.ActivityRecognition,
    "android.hardware.sensor.stepdetector" to ApkAnalyzerIcons.ActivityRecognition,
    "android.hardware.sensor.heartrate" to ApkAnalyzerIcons.Sensors,
    "android.hardware.fingerprint" to ApkAnalyzerIcons.Fingerprint,
    "android.hardware.biometrics.face" to ApkAnalyzerIcons.Fingerprint,
    "android.hardware.usb.host" to ApkAnalyzerIcons.Usb,
    "android.hardware.usb.accessory" to ApkAnalyzerIcons.Usb,
    "android.hardware.audio.output" to ApkAnalyzerIcons.Speaker,
    "android.hardware.audio.low_latency" to ApkAnalyzerIcons.Speaker,
    "android.hardware.audio.pro" to ApkAnalyzerIcons.Speaker,
    "android.hardware.gamepad" to ApkAnalyzerIcons.Gamepad,
    "android.hardware.vulkan.level" to ApkAnalyzerIcons.Graphics,
    "android.hardware.vulkan.version" to ApkAnalyzerIcons.Graphics,
    "android.hardware.opengles.aep" to ApkAnalyzerIcons.Graphics,
    "android.hardware.consumerir" to ApkAnalyzerIcons.Sensors,
    "android.hardware.ram.low" to ApkAnalyzerIcons.Memory,
    "android.hardware.ram.normal" to ApkAnalyzerIcons.Memory,
    "android.hardware.type.watch" to ApkAnalyzerIcons.Watch,
    "android.hardware.type.television" to ApkAnalyzerIcons.Television,
    "android.hardware.type.pc" to ApkAnalyzerIcons.Screen,
    "android.hardware.vr.high_performance" to ApkAnalyzerIcons.Graphics,
    "android.hardware.vr.headtracking" to ApkAnalyzerIcons.Graphics,
    "android.software.vr.mode" to ApkAnalyzerIcons.Graphics,
    "android.software.print" to ApkAnalyzerIcons.Print,
    "android.software.midi" to ApkAnalyzerIcons.Speaker,
    "android.software.leanback" to ApkAnalyzerIcons.Television,
    "android.software.app_widgets" to ApkAnalyzerIcons.Apps,
    "android.software.home_screen" to ApkAnalyzerIcons.Apps,
    "android.software.device_admin" to ApkAnalyzerIcons.Lock,
    "android.software.verified_boot" to ApkAnalyzerIcons.Verified,
    "android.software.picture_in_picture" to ApkAnalyzerIcons.Screen,
    "android.software.backup" to ApkAnalyzerIcons.Storage,
    "android.software.sip.voip" to ApkAnalyzerIcons.Phone,
    "android.software.connectionservice" to ApkAnalyzerIcons.Phone,
)
