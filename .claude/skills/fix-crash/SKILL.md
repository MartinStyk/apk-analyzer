---
name: fix-crash
description: Use to fix a specific production crash or non-fatal — pulling its Crashlytics data, finding the linked GitHub issue, root-causing it, reproducing it, fixing it, and verifying the fix without breaking stored-data compatibility. Triggered by phrases like "fix this crash", "fix issue #42", "root cause this crash", "why does this crash happen", "reproduce this crash", "fix the Crashlytics issue".
---

# Skill: Root-Cause, Reproduce, and Fix a Production Crash

> Takes one Crashlytics issue (or the GitHub issue tracking it) from symptom to a verified fix,
> without breaking users whose stored data was written by an older release.

Finding *which* crashes to work on is a different skill: `triage-crashes`. This one fixes a single
known crash. Read `triage-crashes` for how the Crashlytics MCP bridge works — this skill uses the
same `scripts/crashlytics.js` and the same appId.

## Step 1 — Resolve the crash and its GitHub issue

You may be given a Crashlytics issue id, a console URL, a GitHub issue number, or a description.
Get to a `(crashlyticsIssueId, githubIssueNumber)` pair, where either may be absent:

| Given | Do |
|---|---|
| Crashlytics id or console URL | `crashlytics_list_notes` for a `github.com/.../issues/<n>` link; if none, `gh issue list --state all --limit 200 --search "<id>"` |
| GitHub issue number | `gh issue view <n> --comments` and find the Crashlytics id in the body or comments |
| A description only | Run the `triage-crashes` report queries and match on the exception and frame |

The Crashlytics id is the join key in both directions — issues filed by `triage-crashes` carry it in
their body and a note pointing back. If you find a GitHub issue with no Crashlytics note, add the
note now with `crashlytics_create_note`.

If **no** GitHub issue exists, file one with `create_issue` (label `bug`) using the `triage-crashes`
body format before starting work, so the fix has something to close.

## Step 2 — Pull everything the platform knows

```json
[
  { "name": "crashlytics_get_issue", "arguments": { "appId": "1:636588470688:android:c27a676c8deb5eb0", "issueId": "<id>" } },
  { "name": "crashlytics_list_events", "arguments": { "appId": "...", "pageSize": 5, "filter": { "issueId": "<id>" } } },
  { "name": "crashlytics_get_report", "arguments": { "appId": "...", "report": "topAndroidDevices", "pageSize": 10, "filter": { "issueId": "<id>" } } },
  { "name": "crashlytics_get_report", "arguments": { "appId": "...", "report": "topOperatingSystems", "pageSize": 10, "filter": { "issueId": "<id>" } } },
  { "name": "crashlytics_get_report", "arguments": { "appId": "...", "report": "topVersions", "pageSize": 10, "filter": { "issueId": "<id>" } } }
]
```

Read **several** events, not just one. One stack tells you where it broke; several tell you what the
occurrences share. Look at each event's breadcrumbs, custom keys, and device state.

The distribution is evidence, and it usually names the root cause on its own:

| Pattern | What it means |
|---|---|
| One OS version, or everything below/above an API level | An API availability or behaviour change — check the runtime SDK guard around that call or use a backward-compatible API |
| One manufacturer | An OEM-modified framework; often needs a defensive path, not a "correct" fix |
| Starts exactly at one app version | A regression — `git log` between that tag and the previous one |
| Spread evenly across everything | Logic or state bug, not environmental |

`firstSeenVersion` plus `git log <prev-tag>..<that-tag>` is the fastest route to a regression's cause.

## Step 3 — Root cause it in this repo

Find the deepest `sk.styk.martin.apkanalyzer.*` frame, map its package to a module (see `AGENTS.md`),
read that module's `AGENTS.md`, then read the actual code. Then state the root cause as one sentence
naming the specific condition — "`ON CONFLICT` upsert syntax requires SQLite 3.24, which is absent
below API 30", not "a database error".

Do not start editing until you can state that sentence. If the evidence doesn't support one, say so
and pull more events rather than guessing.

Watch for these, which this codebase has actually produced:

* A framework symbol that doesn't exist on older API levels (`NoSuchMethodError`, `NoSuchFieldError`)
  — the compile-time SDK has it, the runtime device doesn't.
* Raw SQL in a Room `@Query` using syntax newer than the device's bundled SQLite.
* A `LazyColumn` item key that isn't unique across the whole list.
* A `SecurityException` from a package the caller can't inspect on newer Android.

## Step 4 — Reproduce before fixing

An unreproduced fix is a guess. Reproduce in the cheapest way that actually exercises the failure:

1. **Match the environment.** If the distribution points at an API level, start an emulator at that
   exact level — a fix verified only on your daily driver proves nothing about an API-29 crash. Use
   the `run-app` skill to build, install and launch, and `navigate-app-adb` to drive to the screen.
2. **Reach the state.** Breadcrumbs and custom keys on the event tell you what the user did. For a
   crash needing specific stored data, seed it via the app's own flows, or with `adb shell` against
   the app's database — never by adding production code that fabricates the state.
3. **Confirm the same stack.** `adb logcat` must show the *same* exception at the *same* frame. A
   different crash on the same screen is a different bug.

If it genuinely can't be reproduced locally (OEM-specific, a device you don't have), say so
explicitly, and compensate: write the fix so the failing path is provably impossible (a version
guard, a supported API) rather than probabilistically better, and prove the guard with the API level
you *can* run.

Never add a debug hook, test-only flag, or logging to production code to force a repro. Remove any
temporary scaffolding before finishing.

## Step 5 — Fix it

Follow `AGENTS.md` and the module's own `AGENTS.md`. Beyond those:

* Fix the cause, not the symptom. A `try/catch` around a crash that leaves the feature silently
  broken is not a fix — `AGENTS.md` forbids hiding errors behind broad exception handling. Catching
  is only correct when the exception is genuinely expected and the app has a real answer for it.
* Prefer the platform-supported API over a version-guarded fork when one exists.
* If the same broken pattern appears elsewhere, fix every occurrence — grep for it. Shipping a fix
  for one of three identical DAOs means two more crash reports next release.

## Step 6 — Do not break stored data

**Mandatory whenever the fix touches persistence** — Room entities, DAOs, migrations, DataStore,
SharedPreferences, or any serialized model. Users upgrade in place; their existing data was written
by the old code.

* **Schema change** → a Room migration is required. Never bump `version` without one, and never
  resort to `fallbackToDestructiveMigration` — that silently deletes user data.
* **A migration already shipped** → it is immutable. Users have run it. Add a new one; never edit it.
* **Entity/serialized model field change** → adding a nullable field with a default is safe. Removing
  or renaming a field, or narrowing a type, breaks old rows — migrate them.
* **DataStore / preferences key change** → old keys persist. Either keep reading the old key, or
  migrate the value on first read. Silently ignoring it resets the user's settings.
* **Query-only fix** (this is the common case) → confirm the new SQL reads the *existing* schema
  correctly, including rows written before the fix.

Then verify with an actual upgrade, not a clean install: install the previous release, use the app
enough to write data, install the fixed build over it **without uninstalling**, and confirm the old
data still reads correctly and the crash is gone. `adb install -r` preserves data; `adb uninstall`
destroys the evidence and turns this check into theatre.

## Step 7 — Verify

1. Re-run the repro from Step 4 on the same emulator/API level. The crash must be gone and the
   feature must actually work — not just not crash.
2. Check the neighbouring paths the change touched.
3. Build: `./gradlew :<module>:compileDebugKotlin`, then `./gradlew spotlessApply` before committing.
4. Check by hand for imports left behind by deleted code — `spotlessCheck` doesn't flag those.
5. For any visual change, look at it on a device; a clean compile proves nothing about layout.

## Step 8 — Close the loop

* Commit referencing the GitHub issue (`Fixes #<n>`). Author as the human user only — see the
  `git-commit-author` skill.
* Comment on the GitHub issue with the root cause, how it was reproduced, what the fix does, and how
  it was verified — including the upgrade check if persistence was touched.
* Add a Crashlytics note recording the fix and the commit/PR.
* **Do not** close the Crashlytics issue with `crashlytics_update_issue`. It is fixed when a release
  containing the fix stops producing events, which you can't know yet. Note it and move on.

## Verification

- [ ] Crashlytics issue and GitHub issue are linked in both directions
- [ ] Multiple events read, not just one sample
- [ ] Device/OS/version distribution examined for the environmental pattern
- [ ] Root cause stated in one sentence naming the specific condition
- [ ] Reproduced on an environment matching the crash — or the inability stated explicitly, with a
      guard-based fix compensating
- [ ] Fix addresses the cause; no exception swallowed to silence it
- [ ] Grepped for the same pattern elsewhere and fixed every occurrence
- [ ] Persistence changes carry a migration; existing keys/fields still read
- [ ] Verified by upgrading over the previous release, not by a clean install
- [ ] Crash gone on re-run and the feature actually works
- [ ] `spotlessApply` run; no orphaned imports
- [ ] GitHub issue commented, Crashlytics note added, Crashlytics state left alone
