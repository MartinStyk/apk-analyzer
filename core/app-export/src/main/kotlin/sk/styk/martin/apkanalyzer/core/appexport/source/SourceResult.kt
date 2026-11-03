package sk.styk.martin.apkanalyzer.core.appexport.source

internal sealed interface SourceResult<out T> {
    data object NotRequested : SourceResult<Nothing>

    data object Failed : SourceResult<Nothing>

    data class Loaded<T>(val value: T) : SourceResult<T>
}

internal fun <T> SourceResult<T>.valueOrNull(): T? = (this as? SourceResult.Loaded)?.value

internal fun <T> Result<T>.toSourceResult(): SourceResult<T> = fold(
    onSuccess = { SourceResult.Loaded(it) },
    onFailure = { SourceResult.Failed },
)

internal fun <T : Any> Result<T?>.toAvailableSourceResult(): SourceResult<T> = fold(
    onSuccess = { value -> if (value == null) SourceResult.Failed else SourceResult.Loaded(value) },
    onFailure = { SourceResult.Failed },
)
