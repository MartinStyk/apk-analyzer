package sk.styk.martin.apkanalyzer.core.common.configuration

sealed interface ConfigurationKey<T : Any> {
    data object InventoryExportEnabled : ConfigurationKey<Boolean>

    data object WhatChangedTabEnabled : ConfigurationKey<Boolean>

    data object AppDetailHistoryEnabled : ConfigurationKey<Boolean>

    data object AppHistoryRecordingEnabled : ConfigurationKey<Boolean>
}
