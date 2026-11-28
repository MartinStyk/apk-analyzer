package sk.styk.martin.apkanalyzer.core.apps.api.components

data class DisabledComponents(
    val activities: Set<String> = emptySet(),
    val services: Set<String> = emptySet(),
    val receivers: Set<String> = emptySet(),
    val providers: Set<String> = emptySet(),
) {
    fun contains(name: String, kind: ComponentKind): Boolean = name in when (kind) {
        ComponentKind.Activity -> activities
        ComponentKind.Service -> services
        ComponentKind.Receiver -> receivers
        ComponentKind.Provider -> providers
    }

    companion object {
        val None = DisabledComponents()
    }
}
