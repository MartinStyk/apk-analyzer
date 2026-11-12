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
            ::ViewModelContract,
            ::ContractTypeShape,
            ::MutableFlowExposure,
            ::EntryBlockDelegatesToScreen,
            ::NavKeyShape,
            ::ApiModuleContents,
            ::ImplVisibility,
            ::PreviewShape,
            ::PreviewPresence,
            ::NoRawColor,
            ::HardcodedComposeString,
        ),
    )
}
