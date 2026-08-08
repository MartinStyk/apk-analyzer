package sk.styk.martin.apkanalyzer.core.apps.model

data class ComponentIntentFilterKey(val name: String, val kind: ComponentKind)

enum class ComponentKind {
    Activity,
    Service,
    Receiver,
    Provider,
}

data class ComponentIntentFilter(
    val actions: List<String>,
    val categories: List<String>,
    val dataRules: List<IntentFilterDataRule>,
    val uriRelativeGroups: List<IntentFilterUriRelativeGroup>,
    val priority: Int,
    val order: Int,
    val isAutoVerify: Boolean,
)

data class IntentFilterUriRelativeGroup(val isAllowed: Boolean, val dataRules: List<IntentFilterDataRule>)

data class IntentFilterDataRule(val type: IntentFilterDataRuleType, val value: String)

enum class IntentFilterDataRuleType(val manifestAttribute: String) {
    Scheme("scheme"),
    Host("host"),
    Port("port"),
    Path("path"),
    PathPrefix("pathPrefix"),
    PathSuffix("pathSuffix"),
    PathPattern("pathPattern"),
    PathAdvancedPattern("pathAdvancedPattern"),
    SchemeSpecificPart("ssp"),
    SchemeSpecificPartPrefix("sspPrefix"),
    SchemeSpecificPartSuffix("sspSuffix"),
    SchemeSpecificPartPattern("sspPattern"),
    SchemeSpecificPartAdvancedPattern("sspAdvancedPattern"),
    Query("query"),
    QueryPrefix("queryPrefix"),
    QuerySuffix("querySuffix"),
    QueryPattern("queryPattern"),
    QueryAdvancedPattern("queryAdvancedPattern"),
    Fragment("fragment"),
    FragmentPrefix("fragmentPrefix"),
    FragmentSuffix("fragmentSuffix"),
    FragmentPattern("fragmentPattern"),
    FragmentAdvancedPattern("fragmentAdvancedPattern"),
    MimeType("mimeType"),
    MimeGroup("mimeGroup"),
}
