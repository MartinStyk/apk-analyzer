package sk.styk.martin.apkanalyzer.core.appanalysis.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Class holding basic application metadata used in application detail view
 */

@Parcelize
data class AppDetailData(
    val analysisMode: AnalysisMode,
    val generalData: GeneralData,
    val certificateData: CertificateData,
    val activityData: List<ActivityData>,
    val serviceData: List<ServiceData>,
    var contentProviderData: List<ContentProviderData>,
    var broadcastReceiverData: List<BroadcastReceiverData>,
    var permissionData: PermissionDataAggregate,
    var featureData: List<FeatureData>,
) : Parcelable {
    enum class AnalysisMode {
        INSTALLED_PACKAGE,
        APK_FILE,
    }
}
