package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

class ApiModuleContents(config: Config) :
    Rule(
        config,
        "A feature api module holds only @Serializable NavKeys, the types they carry and their serializers.",
    ) {

    override fun visit(root: KtFile) {
        super.visit(root)
        root.declarations.filterNot { it.isAllowedInApi() }.forEach { declaration ->
            report(
                declaration,
                "A feature api module holds only @Serializable NavKeys and the types they carry. Move " +
                    "'${declaration.name.orEmpty()}' to the feature's domain or phone module, or to core (AGENTS.md > Module Rules).",
            )
        }
    }

    private fun KtDeclaration.isAllowedInApi(): Boolean = this is KtClassOrObject && (hasAnnotation("Serializable") || "KSerializer" in superTypeNames())
}
