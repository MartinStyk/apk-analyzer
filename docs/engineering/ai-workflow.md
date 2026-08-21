# AI-assisted workflow

This repository is written to be worked on by humans and by coding agents from the same context
files, and the structure that makes that work is validated by a Gradle task rather than by
convention alone. If you never use an agent, most of this still matters: the `AGENTS.md` files are
the module documentation, and `validateAgentContext` gates them in CI.

## The context files

| File | Role |
|---|---|
| [`AGENTS.md`](../../AGENTS.md) (root) | Canonical engineering instructions: non-negotiables, workflow, module rules, architecture, Compose and naming conventions |
| `<module>/AGENTS.md` | That module's boundary, package map, required patterns, and non-obvious behaviour |
| `CLAUDE.md` (next to every `AGENTS.md`) | Contains exactly `@AGENTS.md` — an import, never a copy |
| [`.github/copilot-instructions.md`](../../.github/copilot-instructions.md) | Thin Copilot adapter that points at `AGENTS.md` and `.claude/skills/` |
| [`.claude/skills/`](../../.claude/skills) | Step-by-step procedures for recurring tasks, shared by Claude and Copilot |

The rule that keeps this maintainable: **guidance lives in the closest relevant `AGENTS.md` and is
never copied into a tool-specific file.** One source, many readers.

## What goes in a module's AGENTS.md

A scoped `AGENTS.md` documents durable context that can't be recovered cheaply by reading the code:
the module's boundary, its package or domain map, patterns it requires, behaviour that would look
wrong without explanation, and entry points that are exceptions.

It deliberately does *not* contain exhaustive file trees, copied interface definitions, ordinary
dependency lists, or implementation summaries — repository search recovers those more accurately,
and they go stale within a release. A specific file is named only when it is a canonical reference,
an assembly point, or misleadingly named.

The practical test: **adding or renaming an ordinary source file should not require an `AGENTS.md`
update.** If it does, the file is documenting the wrong layer.

## When they get written

* A new module gets its `AGENTS.md` and `CLAUDE.md` as part of the module scaffold — the
  [`create-core-module`](../../.claude/skills/create-core-module/SKILL.md) and
  [`create-feature-module`](../../.claude/skills/create-feature-module/SKILL.md) skills include the
  step, and `validateAgentContext` fails the build if it's missing.
* An existing module's `AGENTS.md` is updated in the same PR as a change to its package structure,
  a new required pattern, or a boundary shift — not in a follow-up.
* The root `AGENTS.md` changes only when a repository-wide rule changes.

## What `validateAgentContext` checks

[`ValidateAgentContextTask`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateAgentContextTask.kt),
registered by
[`AgentContextPlugin`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/AgentContextPlugin.kt)
on the root project:

* **Pairing** — every `AGENTS.md` has a sibling `CLAUDE.md` containing exactly `@AGENTS.md`, and
  every `CLAUDE.md` has a sibling `AGENTS.md`.
* **Module coverage** — every Gradle module in
  [`settings.gradle.kts`](../../settings.gradle.kts) has a module-scoped `AGENTS.md` of its own;
  inheriting the root one is an error.
* **Skill metadata** — every `.claude/skills/<name>/` contains a `SKILL.md` with valid YAML
  frontmatter whose `name` matches the directory, is lowercase `[a-z0-9-]`, and whose `description`
  is 1–1024 characters.
* **No duplicate skill locations** — a skill must not be mirrored into `.github/skills/`,
  `.agents/skills/`, or `.github/prompts/*.prompt.md`.
* **Copilot adapter present** — `.github/copilot-instructions.md` must exist.
* **Links resolve** — every local Markdown link in any `AGENTS.md`, `CLAUDE.md`, `SKILL.md`, the
  root `README.md`, or the Copilot adapter must point at a file that exists.

Run it with `./gradlew validateAgentContext`.
[`.github/workflows/agent-context.yml`](../../.github/workflows/agent-context.yml) runs it on every
push and PR to `develop` that touches a context file, a skill, the build logic, the version catalog,
or `settings.gradle.kts`.

Note that link validation covers the root `README.md` but not the files under `docs/` — check links
in a doc change by hand.

## The skills

A skill is a procedure, not a description. Each one's frontmatter states the phrases that should
trigger it, and the body is the ordered steps for a task that is otherwise easy to get subtly wrong
by copying an existing file.

| Skill | Use it when |
|---|---|
| [`create-feature-module`](../../.claude/skills/create-feature-module/SKILL.md) | Adding a feature area — creates the `api`/`impl` pair, plugins, package structure, nav wiring |
| [`create-core-module`](../../.claude/skills/create-core-module/SKILL.md) | Adding a domain/data module — interface + `internal Impl` + Hilt binding + `AGENTS.md` |
| [`create-compose-component`](../../.claude/skills/create-compose-component/SKILL.md) | A feature needs a UI component that doesn't exist in `core:ui-library` yet |
| [`implement-navigation`](../../.claude/skills/implement-navigation/SKILL.md) | Adding a destination, NavKey, or entry provider, or navigating between features |
| [`spotless-fix`](../../.claude/skills/spotless-fix/SKILL.md) | Formatting or ktlint violations that `spotlessApply` doesn't resolve outright |
| [`translate-strings`](../../.claude/skills/translate-strings/SKILL.md) | Adding or changing user-facing copy, or auditing a locale |
| [`run-app`](../../.claude/skills/run-app/SKILL.md) | Verifying a change actually works at runtime, not just that it compiles |
| [`navigate-app-adb`](../../.claude/skills/navigate-app-adb/SKILL.md) | Driving the UI on a device via adb to reach and inspect a screen |
| [`analyze-ci-failure`](../../.claude/skills/analyze-ci-failure/SKILL.md) | A workflow run failed and you want the actual failing step, not the log |
| [`setup-local-tools`](../../.claude/skills/setup-local-tools/SKILL.md) | Setting up a new machine, including headless/CLI-only setup |
| [`git-commit-author`](../../.claude/skills/git-commit-author/SKILL.md) | Any commit — commits are authored by the human contributor only |
| [`sync-design-changes`](../../.claude/skills/sync-design-changes/SKILL.md) | Bringing tweaks from the Claude Design project back into Compose code |

All are shared by Claude and Copilot except `sync-design-changes`, which needs Claude's `DesignSync`
tool.

## Working with an agent in this repository

The workflow the root [`AGENTS.md`](../../AGENTS.md) asks for, human or agent:

1. Understand the requirement and its acceptance criteria.
2. Search for an existing implementation of similar behaviour before designing a new one.
3. Read the `AGENTS.md` of every module you'll touch.
4. Plan, then implement the smallest clean solution that fits the existing architecture.
5. Run `./gradlew spotlessApply`, plus a single-module compile while iterating.
6. Review the complete diff — including anything changed earlier in the worktree — before opening a
   PR.

Two constraints worth restating, because they are the ones an agent most often violates:

* **Commits are authored as the human contributor only.** No `Co-Authored-By` AI trailers, no
  session metadata.
* **No new dependencies, no new tests, no new abstractions** without asking first.

## Related

* [Architecture](architecture.md) · [Coding standards](coding-standards.md) ·
  [Verification](verification.md)
* [`CONTRIBUTING.md`](../../CONTRIBUTING.md) — the human PR workflow
