package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction

class SingletonRepositoryBinding(config: Config) :
    Rule(
        config,
        "Repositories and Managers are bound with @Binds and scoped @Singleton.",
    ) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.hasAnnotation("Binds") || function.hasAnnotation("Singleton")) return
        val boundType = function.typeReference?.baseTypeName() ?: return
        if (boundType.endsWith("Repository") || boundType.endsWith("Manager")) {
            report(
                function.nameIdentifier ?: function,
                "Scope the $boundType binding with @Singleton (AGENTS.md > Architecture > Data Layer).",
            )
        }
    }
}
