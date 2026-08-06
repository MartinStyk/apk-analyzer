package sk.styk.martin.apkanalyzer.feature.appdetail.impl.requirements

sealed interface RequirementsAction {
    data object Retry : RequirementsAction

    data class CopyValue(val label: String, val value: String) : RequirementsAction
}
