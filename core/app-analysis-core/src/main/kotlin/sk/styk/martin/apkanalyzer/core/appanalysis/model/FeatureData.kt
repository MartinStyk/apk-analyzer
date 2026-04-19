package sk.styk.martin.apkanalyzer.core.appanalysis.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FeatureData(val name: String, val isRequired: Boolean = false) : Parcelable
