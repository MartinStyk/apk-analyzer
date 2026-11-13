# detekt-rules Module

## Purpose

The custom detekt rule set `apkanalyzer`. These rules turn the conventions in the root
[`AGENTS.md`](../../AGENTS.md) into build failures, so code that drifts from them can't merge. Every
check, custom or built-in, is listed in
[`docs/engineering/guardrails.md`](../../docs/engineering/guardrails.md).

## Structure

* One rule per file, or a few closely related rules sharing a file (`PreviewRules.kt`). PSI helpers
  shared by several rules live in `Psi.kt`.
* `ApkAnalyzerRuleSetProvider` registers every rule. It is loaded through
  `META-INF/services/dev.detekt.api.RuleSetProvider`.
* `src/main/resources/config/config.yml` lists every rule as active. Path scopes (`includes` and
  `excludes`) live only in the repository's [`.detekt/detekt.yml`](../../.detekt/detekt.yml) under
  `apkanalyzer:`, never in rule code.

## Writing or Changing a Rule

* Write a rule only when nothing else catches the drift. Check the compiler and classpath, ktlint and
  compose-rules, built-in detekt rules (`ForbiddenImport` and `ForbiddenMethodCall` cover most bans),
  and Android lint first.
* A rule reports what to write instead and names the `AGENTS.md` section it enforces.
* Match on syntax only; the custom rules don't use type resolution. When a convention can't be
  checked from syntax, prefer a type-resolved built-in rule, or leave it to review.
* A new rule must hold for the whole codebase on the day it lands. Fix the existing violations in the
  same change. There are no baselines.
* Every rule has tests in `src/test`: at least one compliant snippet and one violation, plus a case
  for each false positive fixed so far. Run them with `./gradlew -p build-logic :detekt-rules:test`.
* Register the rule in the provider, `config.yml`, `ForbiddenSuppress` in `.detekt/detekt.yml`, and
  `docs/engineering/guardrails.md`.

## Non-Obvious Behavior

* The Gradle daemon caches the detekt plugin classloader. After changing rule code, run
  `./gradlew --stop` before `detektDebug`, or the old rules jar runs without any error.
* This project uses the detekt 2 API (`dev.detekt.api`). Detekt 1 examples online use different
  package names and a different `Rule` constructor.
