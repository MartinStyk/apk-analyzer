---
name: verify
description: Unified IDE-local verification gate for ApkAnalyzer. Diff-vs-base review pipeline — skip-check → context discovery → parallel reviewers (bug + convention + security) → issue-validator → local build gates (spotlessApply, per-module compile, conditional validateAgentContext/full whole-app check) → verify-audit → high-signal report (HIGH-confidence BLOCKER/MAJOR only).
argument-hint: "[--base=<ref>] [--full]"
user-invocable: true
disable-model-invocation: false
---

# /verify — orchestrated IDE-local code-review + build gate

You are the **main agent**. You orchestrate all sub-agent dispatches. Sub-agents under
`.claude/agents/` cannot dispatch further sub-agents (Claude Code architectural constraint). All
parallel fan-out happens from your context.

This command is the single verification gate for any finished piece of work in this repo — a
module addition, a bug fix, a scratch experiment. It diffs the working branch against a base ref
and runs a tight, high-signal review grounded in this repo's `AGENTS.md` files. Only issues that
any contributor who knows [`AGENTS.md`](../../../AGENTS.md) would say "yes, that has to be fixed
before this is pushed" reach the final report.

This is heavier than the "don't run these after every edit" guidance in root `AGENTS.md` — that
guidance is about *not gating local iteration*. `/verify` is what you run once, deliberately,
before opening a PR, in place of running the gates by hand.

## Arguments

The user invoked: `/verify $ARGUMENTS`

`$ARGUMENTS` is optional. Recognized flags (any order, both optional):

- `--base=<ref>` — git ref to diff against. Default: `develop` (this repo's main branch).
- `--full` — also run the whole-app check (`spotlessCheck detektDebug
  :build-logic:convention:detektMain lintDebug :app:assembleDebug`) in Stage 5, matching what CI
  gates on. Without it, Stage 5 only runs `spotlessApply` plus a compile of the touched modules —
  fast, matching the "iterate on a single-module compile" guidance in root `AGENTS.md`.

Any other token, any malformed flag, or any unrecognized form → emit `INVALID_INVOCATION` (Stage 8
format) with the offending token in `Notes` and stop.

## Sub-agent registry

| name | model | role |
|---|---|---|
| `skip-checker` | haiku | Stage 1 — decides SKIP yes/no |
| `bug-reviewer` | opus | Stage 4 — logic bug pass over the diff |
| `convention-reviewer` | sonnet | Stage 4 — convention compliance grounded in `AGENTS.md` / `docs/engineering/` |
| `security-reviewer` | opus | Stage 4 — security and behavior deviation |
| `issue-validator` | opus | Stage 6 — confirms each emitted issue independently |
| `verify-audit` | sonnet | Stage 9 — discipline pass over the initial report; emits per-finding KEEP / CUT / UNSURE |

Dispatch them with `subagent_type: "<name>"` on the `Agent` tool.

---

## Stage 0 — Parse arguments

1. Tokenize `$ARGUMENTS`. Empty → defaults (`base=develop`, `full=false`).
2. For each token, match `--base=<ref>` or `--full`. Anything else → `INVALID_INVOCATION` (Stage 8
   format) and stop.

## Stage 1 — Skip check

Compute `changed_files = git diff <base>...HEAD --name-only`. If empty, emit `SKIPPED` (Stage 8
format) with reason "no diff vs <base>" and stop.

Otherwise dispatch **one** `skip-checker` `Agent` call. Pass `diff_command` (e.g.
`git diff <base>...HEAD`) and `changed_files` (newline-separated).

If the agent returns `{"skip": true, ...}`, jump straight to Stage 8 with verdict `SKIPPED` and the
agent's reason in `Notes`. Do not proceed past Stage 1.

## Stage 2 — Context discovery (orchestrator, sequential reads)

Read into your context (these are ground truth for downstream leaves). Collect their absolute paths
in a `discovered_files` list:

- Root [`AGENTS.md`](../../../AGENTS.md) (always).
- [`docs/engineering/coding-standards.md`](../../../docs/engineering/coding-standards.md) and
  [`docs/engineering/architecture.md`](../../../docs/engineering/architecture.md) (always — these
  expand the root `AGENTS.md` rules with the worked examples reviewers can quote).
- For every file in `changed_files`, the **nearest module-scoped `AGENTS.md`**: walk up from the
  changed file's directory until you find an `AGENTS.md` other than the root one (same algorithm
  `validateAgentContext` uses to bind a Gradle module to its `AGENTS.md`). Deduplicate.

Also record, from the same scan:

- `changed_modules` — the set of Gradle module directories touched (walk up from each changed
  `.kt`/`.kts` file to the nearest directory containing a `build.gradle.kts`).
- `context_files_touched` — `true` if any changed file is an `AGENTS.md`, a `CLAUDE.md`, anything
  under `.claude/skills/`, `.github/copilot-instructions.md`, anything under `build-logic/`,
  `settings.gradle.kts`, or a library/plugin/bundle entry (not just a version bump) in
  `gradle/libs.versions.toml`.

## Stage 3 — Diff summary (orchestrator, no sub-agent)

Run `git diff <base>...HEAD <files>` and produce a 5–10 line factual summary of what changed. Keep
it short — it's an aid for the reviewers, not user output.

## Stage 4 — Parallel review fan-out (SINGLE MESSAGE, MANY AGENT CALLS)

In **one assistant turn**, dispatch the following in parallel — separate `Agent` tool calls within
the same message:

1. `bug-reviewer` × 1. Payload: `diff`, `diff_summary`, `discovered_files`.
2. `convention-reviewer` × 1. Payload: same.
3. `security-reviewer` × 1. Payload: same.

If any `*Test.kt` file is in the diff (an explicitly-requested exception to the "no test
infrastructure" rule), it's part of `diff` and is reviewed by all three under the same rules as
production code.

**Hard rule:** parallel `Agent` calls in one assistant turn. Sequential dispatch is a bug.

Collect every JSON response. Build a single `findings` list of issues (each tagged with its origin
`category`).

## Stage 5 — Local build gates

1. **Always:** `./gradlew spotlessApply` — the one gate root `AGENTS.md` requires before every
   commit. If it rewrites files, note which ones in `Notes`; those rewrites are not separately
   re-reviewed by Stage 4.
2. **Always, per module in `changed_modules`:** `./gradlew <module>:compileDebugKotlin` (use
   `<module>:compileKotlin` for a module with no Android debug variant).
3. **Conditional — `context_files_touched` is `true` OR `--full` was passed:**
   `./gradlew validateAgentContext`.
4. **Conditional — `--full` was passed:** the whole-app check —
   `./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug :app:assembleDebug`.

Capture PASS / FAIL and the full failing-task output verbatim for every task actually run.

## Stage 6 — Per-issue validation (SINGLE MESSAGE, PARALLEL)

Dedupe `findings` by `(file, line, title)`. For every surviving issue, dispatch **one**
`issue-validator` `Agent` call in a single turn — passing only that one `issue` object (not the
full diff, not other findings).

Filter the validated set to **HIGH-confidence + severity ∈ {BLOCKER, MAJOR}**. Everything else is
dropped (any MEDIUM- or LOW-confidence result, and any MINOR / NIT severity, even when validated).
The surviving set is what you report and what feeds the verdict.

## Stage 7 — Aggregate initial verdict + write report to /tmp

Initial verdict priority (first match wins):

1. `INVALID_INVOCATION` — Stage 0 rejected args.
2. `SKIPPED` — Stage 1 said skip or empty diff.
3. `GATES_FAILED` — any Stage 5 task failed.
4. `FIXES_REQUIRED` — Stage 6 surviving findings include any BLOCKER or MAJOR.
5. `PASS` — none of the above.

### Write the initial report to /tmp (input for Stage 8)

If the initial verdict is `SKIPPED` or `INVALID_INVOCATION`, skip the rest of Stage 7 and skip
Stage 8 entirely — there are no findings to audit. Emit the final output (Stage 9 format) directly
using the initial verdict and the `Notes` reason.

Otherwise:

```bash
mkdir -p /tmp/apkanalyzer-verify.$$
```

`Write` the following to `/tmp/apkanalyzer-verify.$$/report.md`:

```
## Verdict
<initial verdict>

## Gates
<one line per Gradle task actually run: `PASS <task>` or `FAIL <task>` + indented failing output>

## Findings
<numbered list; each item: severity, category, file:line, title, evidence, explanation, rule_citation if convention>

## Notes
<anything else, including any spotlessApply rewrites from Stage 5>
```

Number every finding sequentially across categories (1, 2, 3, …). The numbers are what Stage 8's
audit table references.

## Stage 8 — Verify-audit discipline pass (SINGLE AGENT CALL)

Skipped when Stage 7 short-circuited at `SKIPPED` or `INVALID_INVOCATION`.

Dispatch **one** `verify-audit` `Agent` call. Payload:

- `report_path` — `/tmp/apkanalyzer-verify.$$/report.md`.
- `audit_output_path` — `/tmp/apkanalyzer-verify.$$/audit.md`.
- `changed_files` — the Stage 1 `changed_files` list (newline-separated).
- `discovered_files` — the Stage 2 `discovered_files` list (newline-separated absolute paths).
- `worktree_root` — the repo root absolute path (output of `git rev-parse --show-toplevel`).

The agent will `Write` the audit table to `audit_output_path` and reply with that path. `Read` the
file. It contains a markdown table:

```
| # | Finding | Verdict | Trigger | Reason |
|---|---|---|---|---|
| 1 | <finding 1 title> | KEEP | none | ... |
| 2 | <finding 2 title> | CUT | quoted rule doesn't exist | ... |
| 3 | <finding 3 title> | UNSURE | bug finding requires out-of-diff context | ... |
```

## Stage 9 — Apply audit + emit final output

Apply the audit row-by-row to the Stage 7 findings list (numbers match):

- `KEEP` → finding stays in the final report unchanged.
- `CUT` → finding is removed from the final report.
- `UNSURE` → finding stays, but prefix its title with `[UNVERIFIED]` so the user knows the audit
  couldn't confirm it cheaply.

**Recompute the verdict** from the post-audit findings list using the Stage 7 vocabulary. The
verdict can downgrade for reviewer findings — e.g. if every `BLOCKER` was `CUT`, `FIXES_REQUIRED`
can become `PASS`. The verdict does **not** downgrade past `GATES_FAILED`, which is immune to audit
filtering: a failing Gradle task is a fact about the codebase, not a subjective reviewer finding.

Emit the final user-facing output:

```
## Verdict
<recomputed verdict>

## Gates
<one line per Gradle task actually run: `PASS <task>` or `FAIL <task>` + indented failing output>

## Findings
<post-audit findings: KEEP-rated and UNSURE-rated (prefixed `[UNVERIFIED]`). Grouped by category. Omit section if empty.>

## Audit
<the full markdown table from Stage 8, including CUT rows so the user sees what was filtered and why>

## Notes
<anything noteworthy; reason for SKIPPED / INVALID_INVOCATION; brief comment if the audit changed the verdict>
```

Omit sections that are empty / not applicable. Stop after emitting.

## Re-invocation contract

If the recomputed verdict is `FIXES_REQUIRED` or `GATES_FAILED`, address every surviving finding
(or document why a finding is rejected by quoting the discovered rule that contradicts it), then
re-run `/verify` with the same args. Only consider a piece of work shippable / mergeable once
`/verify` returns `PASS` or `SKIPPED`.
