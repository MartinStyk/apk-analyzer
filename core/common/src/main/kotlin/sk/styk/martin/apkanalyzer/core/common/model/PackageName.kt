package sk.styk.martin.apkanalyzer.core.common.model

@JvmInline
value class PackageName(val value: String) {
    override fun toString(): String = value
}
