package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class ApkAnalyzerRuleSetProvider : RuleSetProvider {

    override val ruleSetId: RuleSetId = RuleSetId("apkanalyzer")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::NoComments,
            ::AppFunctionKDocRequired,
            ::NoKtlintSuppression,
            ::Material3Scope,
            ::TvMaterial3Scope,
            ::MaterialIconsScope,
            ::ViewModelContract,
            ::ContractTypeShape,
            ::MutableFlowExposure,
            ::EntryBlockDelegatesToScreen,
            ::NavKeyShape,
            ::ApiModuleContents,
            ::SealedMemberDataObject,
            ::LoggerTag,
            ::ImplVisibility,
            ::ImplSeparateFile,
            ::HiltModuleShape,
            ::RunCatchingInSuspend,
            ::BlockingOrConsoleCall,
            ::PresentTenseCallbacks,
            ::PreviewShape,
            ::PreviewPresence,
            ::NoRawColor,
            ::HardcodedComposeString,
        ),
    )
}
