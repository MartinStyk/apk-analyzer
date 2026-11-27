package sk.styk.martin.apkanalyzer.core.featureflags.api

import kotlinx.serialization.Serializable

@Serializable
enum class FeatureFlag {
    InventoryExport,
    WhatChangedTab,
    AppDetailHistory,
    AppHistoryRecording,
}
