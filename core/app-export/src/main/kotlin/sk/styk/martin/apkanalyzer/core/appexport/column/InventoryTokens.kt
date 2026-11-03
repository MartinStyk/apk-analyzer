package sk.styk.martin.apkanalyzer.core.appexport.column

import sk.styk.martin.apkanalyzer.core.apps.components.ComponentKind
import sk.styk.martin.apkanalyzer.core.apps.components.ProviderPathMatchType
import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionFlag
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.apps.signing.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

internal val AppCategory.token: String?
    get() = when (this) {
        AppCategory.Undefined -> null
        AppCategory.Game -> "game"
        AppCategory.Audio -> "audio"
        AppCategory.Video -> "video"
        AppCategory.Image -> "image"
        AppCategory.Social -> "social"
        AppCategory.News -> "news"
        AppCategory.Maps -> "maps"
        AppCategory.Productivity -> "productivity"
        AppCategory.Accessibility -> "accessibility"
    }

internal val AppSource.token: String
    get() = when (this) {
        AppSource.GooglePlay -> "google_play"
        AppSource.SamsungGalaxyStore -> "samsung_galaxy_store"
        AppSource.AmazonAppstore -> "amazon_appstore"
        AppSource.HuaweiAppGallery -> "huawei_app_gallery"
        AppSource.XiaomiGetApps -> "xiaomi_get_apps"
        AppSource.FDroid -> "f_droid"
        AppSource.AuroraStore -> "aurora_store"
        AppSource.Sideloaded -> "sideloaded"
        AppSource.LocalInstall -> "local_install"
        AppSource.SystemPreinstalled -> "system_preinstalled"
        AppSource.Unknown -> "unknown"
    }

internal val InstallLocation.token: String
    get() = when (this) {
        InstallLocation.Auto -> "auto"
        InstallLocation.Internal -> "internal_only"
        InstallLocation.External -> "prefer_external"
    }

internal val SplitApkKind.token: String
    get() = when (this) {
        SplitApkKind.DynamicFeature -> "dynamic_feature"
        SplitApkKind.Abi -> "abi"
        SplitApkKind.ScreenDensity -> "screen_density"
        SplitApkKind.Language -> "language"
    }

internal val ProtectionLevel.token: String
    get() = when (this) {
        ProtectionLevel.Dangerous -> "dangerous"
        ProtectionLevel.Signature -> "signature"
        ProtectionLevel.Internal -> "internal"
        ProtectionLevel.Normal -> "normal"
    }

internal val ProtectionFlag.token: String
    get() = when (this) {
        ProtectionFlag.Privileged -> "privileged"
        ProtectionFlag.AppOp -> "app_op"
        ProtectionFlag.Instant -> "instant"
        ProtectionFlag.Development -> "development"
    }

internal val CertificateTrustLevel.token: String
    get() = when (this) {
        CertificateTrustLevel.Valid -> "valid"
        CertificateTrustLevel.Debug -> "debug"
    }

internal val ProviderPathMatchType.token: String
    get() = when (this) {
        ProviderPathMatchType.Literal -> "literal"
        ProviderPathMatchType.Prefix -> "prefix"
        ProviderPathMatchType.SimpleGlob -> "simple_glob"
        ProviderPathMatchType.AdvancedGlob -> "advanced_glob"
        ProviderPathMatchType.Suffix -> "suffix"
    }

internal val SigningSchemeVersion.token: String
    get() = when (this) {
        SigningSchemeVersion.V1 -> "v1"
        SigningSchemeVersion.V2 -> "v2"
        SigningSchemeVersion.V3 -> "v3"
        SigningSchemeVersion.V3_1 -> "v3.1"
    }

internal val ComponentKind.token: String
    get() = when (this) {
        ComponentKind.Activity -> "activity"
        ComponentKind.Service -> "service"
        ComponentKind.Receiver -> "receiver"
        ComponentKind.Provider -> "provider"
    }
