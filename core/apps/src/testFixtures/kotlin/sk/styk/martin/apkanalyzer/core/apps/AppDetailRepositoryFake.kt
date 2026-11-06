package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

class AppDetailRepositoryFake(vararg details: AppDetail) : AppDetailRepository {
    private val details = details.associateByTo(mutableMapOf()) { it.reference() }

    operator fun plusAssign(detail: AppDetail) {
        details[detail.reference()] = detail
    }

    override suspend fun details(reference: AppReference): Result<AppDetail> =
        details[reference]?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No details set for $reference"))

    private fun AppDetail.reference(): AppReference = AppReference.InstalledPackage(info.packageName)
}
