# detekt-rules Module

## Purpose

The custom detekt rule set `apkanalyzer`. These rules turn the conventions in the root
[`AGENTS.md`](../../AGENTS.md) into build failures, so code that drifts from them can't merge. The
full catalogue, including rejected and deferred checks, is in
[`docs/engineering/guardrails.md`](../../docs/engineering/guardrails.md).

## Structure

* One rule per file, or a few closely related rules sharing a file (`ScopedPackageRules.kt`,
  `ImplRules.kt`, `PreviewRules.kt`). PSI helpers shared by several rules live in `Psi.kt`.
* `ApkAnalyzerRuleSetProvider` registers every rule. It is loaded through
  `META-INF/services/dev.detekt.api.RuleSetProvider`.
* `src/main/resources/config/config.yml` lists every rule as active. Path scopes (`includes` and
  `excludes`) live only in the repository's [`.detekt/detekt.yml`](../../.detekt/detekt.yml) under
  `apkanalyzer:`, never in rule code.

## Writing or Changing a Rule

* A rule reports what to write instead and names the `AGENTS.md` section it enforces.
* Match on syntax only. The rules run without type resolution, so they can't resolve types or
  follow calls. When a convention can't be checked from syntax alone, document it instead.
* A new rule must hold for the whole codebase on the day it lands. Fix the existing violations in the
  same change. There are no baselines.
* Every rule has tests in `src/test`: at least one compliant snippet and one violation, plus a case
  for each false positive fixed so far. Run them with `./gradlew -p build-logic :detekt-rules:test`.
* Register the rule in the provider, `config.yml`, `.detekt/detekt.yml`, and the guardrails catalogue.

## Non-Obvious Behavior

* The Gradle daemon caches the detekt plugin classloader. After changing rule code, run
  `./gradlew --stop` before `detektDebug`, or the old rules jar runs without any error.
* This project uses the detekt 2 API (`dev.detekt.api`). Detekt 1 examples online use different
  package names and a different `Rule` constructor.
