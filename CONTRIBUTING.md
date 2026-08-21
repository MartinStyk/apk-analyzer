# Contributing to ApkAnalyzer

Thanks for wanting to help. This is a live app with over 2 million downloads, so changes are held to
a production bar — but the bar is documented, not implied.

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## Before you write code

**Read [`AGENTS.md`](AGENTS.md).** It's the canonical engineering reference: module boundaries,
MVVM/DI/navigation conventions, Compose rules, naming, and where a given change belongs. Then read
the `AGENTS.md` of every module you touch — each one documents its own package map and rules.

**Want the reasoning, not just the rules?** [`docs/engineering/`](docs/engineering/README.md) expands
them: [architecture](docs/engineering/architecture.md),
[coding standards](docs/engineering/coding-standards.md),
[verification](docs/engineering/verification.md), and the
[AI-assisted workflow](docs/engineering/ai-workflow.md).

**Check the [roadmap](docs/product/roadmap.md).** Open scope is tracked there with stable IDs, and
finished or deliberately retired work is in [`shipped.md`](docs/product/shipped.md). If your idea is
already retired, the reasoning is recorded — worth reading before re-proposing it.

**Open an issue first for anything non-trivial.** Bug fixes and small improvements can go straight
to a PR. New features, new modules, new dependencies, or architectural changes should be discussed
in an issue first so nobody builds something that doesn't fit.

## Setting up

```bash
git clone https://github.com/MartinStyk/AndroidApkAnalyzer.git
cd AndroidApkAnalyzer
./gradlew assembleDebug
```

Android Studio (latest stable) is the only prerequisite — the Gradle setup provisions a matching JDK
itself. The [`setup-local-tools`](.claude/skills/setup-local-tools/SKILL.md) skill covers headless
setup and optional tooling.

## The rules that matter most

These come from [`AGENTS.md`](AGENTS.md); they're the ones PRs most often trip over.

* **Kotlin, Compose, Hilt, coroutines.** No XML layouts, no `Thread`/`Executor`/`runBlocking`, no
  alternative DI framework.
* **No new dependencies without asking.** [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
  is the only place coordinates and versions live.
* **No comments or KDoc.** Names and structure carry the meaning. This is deliberate and enforced in
  review.
* **No hardcoded user-facing strings.** Use `stringResource` backed by the owning module's
  `strings.xml`.
* **Feature modules never import `androidx.compose.material3`.** Use the wrappers in
  [`core:ui-library`](core/ui-library/AGENTS.md); add one there first if it doesn't exist.
* **Never hardcode SDK levels or the JVM toolchain** in a module's `build.gradle.kts` — they come
  from `build-logic`.
* **Don't add tests or test dependencies** unless the issue explicitly asks. There is no test
  infrastructure here yet, by choice.

Recurring tasks have step-by-step procedures in [`.claude/skills/`](.claude/skills) — creating a
feature module, creating a core module, adding a UI component, wiring navigation, fixing formatting,
running the app on a device. Use them instead of copying an existing file and hoping.

## Before you open a PR

```bash
./gradlew spotlessApply    # required — the only gate you must run locally
```

Then, if you want to pre-empt CI:

```bash
./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug :app:assembleDebug
```

While iterating, a single-module compile is much faster:
`./gradlew :feature:apps:impl:compileDebugKotlin`.

Two things no gate catches, so check them by hand:

* **Unused imports** — Spotless won't flag them.
* **Layout correctness** — a clean compile proves nothing about a Compose screen. For any visual
  change, run it on a device (see the [`run-app`](.claude/skills/run-app/SKILL.md) skill) and look
  at it.

## Pull requests

* Target `develop`. Releases are cut from tags on it.
* Keep the change focused. Unrelated refactors, formatting churn, and drive-by fixes make review
  slower and are usually asked to be split out.
* Describe **what changed and why**, and link the issue or roadmap ID it addresses. Include a
  screenshot or screen recording for anything visual.
* Commits are authored by you as a human contributor. Don't add AI co-author trailers — see the
  [`git-commit-author`](.claude/skills/git-commit-author/SKILL.md) skill.
* Expect review comments about architecture and scope. They're about fit with the codebase, not
  about you.

## Reporting bugs

Include the app version, Android version and device, the app you were inspecting if relevant, what
you expected, what happened, and steps to reproduce. A screenshot usually saves a round trip.

**Do not file security vulnerabilities as public issues** — see [`SECURITY.md`](SECURITY.md).

## Translations

Strings live in each module's `res/values/strings.xml`, with translations in the matching
`values-<locale>` folders — the app currently ships English and Japanese. A PR that only touches
string resources is always welcome and needs no prior issue.

## License

By contributing, you agree that your contributions are licensed under the
[GNU General Public License v3.0](LICENSE).
