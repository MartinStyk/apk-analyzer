package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.appanalysis.model.FeatureData
import javax.inject.Inject

class FeaturesManager
@Inject
constructor() {
    fun getFeatures(packageInfo: PackageInfo): List<FeatureData> = packageInfo.reqFeatures.orEmpty().map {
        FeatureData(
            name = it.name ?: it.glEsVersion,
            isRequired = (it.flags and FeatureInfo.FLAG_REQUIRED) > 0,
        )
    }
}
