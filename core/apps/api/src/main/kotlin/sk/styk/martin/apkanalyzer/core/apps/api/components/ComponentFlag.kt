package sk.styk.martin.apkanalyzer.core.apps.components

enum class ComponentFlag {
    StopWithTask,
    SingleUser,
    IsolatedProcess,
    ExternalService,
}

val Service.flags: List<ComponentFlag>
    get() = buildList {
        if (isStopWithTask) add(ComponentFlag.StopWithTask)
        if (isSingleUser) add(ComponentFlag.SingleUser)
        if (isIsolatedProcess) add(ComponentFlag.IsolatedProcess)
        if (isExternalService) add(ComponentFlag.ExternalService)
    }
