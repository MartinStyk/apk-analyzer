---
name: triage-crashes
description: Use to review production Crashlytics crashes and non-fatals for the latest release and file a GitHub issue for each one that isn't tracked yet. Triggered by phrases like "triage crashes", "check production crashes", "look at Crashlytics", "any new crashes", "review non-fatals", "file issues for the latest release crashes", "what's crashing in production".
---

# Skill: Triage Production Crashes and Non-Fatals

> Reads Crashlytics for the latest released version, decides which issues are worth tracking, and
> files one GitHub `bug` issue per untracked issue — linking both directions so the same crash is
> never filed twice.

Fixing a crash is a **different** skill: `fix-crash`. This skill only triages and files.

## How Crashlytics is read

There is no Crashlytics REST API and `firebase crashlytics:*` only *uploads* symbols. Read access
comes from the Firebase CLI's MCP server, driven by `scripts/crashlytics.js` in this skill folder:

```powershell
node .claude\skills\triage-crashes\scripts\crashlytics.js . <calls.json>
```

`<calls.json>` is an array of `{ "name": ..., "arguments": ... }` tool calls run in order. Write it
to the session artifacts folder, not the repo. Batch several calls into one file — each invocation
pays the CLI's startup cost.

Constants for this project:

| Thing | Value |
|---|---|
| Firebase project | `apkanalyzer-f79a3` |
| `appId` (free flavour, the one that gets traffic) | `1:636588470688:android:c27a676c8deb5eb0` |
| `appId` (premium — only if the user asks) | `1:636588470688:android:89ea2fd0c34abd89` |

Available tools: `crashlytics_get_report`, `crashlytics_get_issue`, `crashlytics_list_events`,
`crashlytics_batch_get_events`, `crashlytics_list_notes`, `crashlytics_create_note`,
`crashlytics_delete_note`, `crashlytics_update_issue`.

If a call fails with `PRECONDITION_FAILED: ... requires an active project`, prepend
`{ "name": "firebase_update_environment", "arguments": { "active_project": "apkanalyzer-f79a3" } }`
to the calls file. If it fails on auth, ask the user to run `firebase login` — never try to
re-authenticate on their behalf.

## Step 1 — Determine the latest released version

**Do not** use the `topVersions` report to pick "latest" — it sorts by event count, so a popular old
version outranks a fresh release. Get the version from git instead:

```powershell
git tag --sort=-v:refname | Select-Object -First 1
```

Release tags are `MAJOR.MINOR.PATCH`; `.github/workflows/release.yml` computes
`versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`. Crashlytics wants the combined
`versionDisplayNames` format — tag `4.0.0` → `"4.0.0 (40000)"`.

Confirm that display name actually appears in a `topVersions` report before filtering on it; if it
doesn't, the release has no events yet (say so and stop) or the tag/versionCode assumption broke.

Bare numeric tags (`58`, `57`) are the pre-4.0 scheme — ignore them when resolving "latest".

## Step 2 — Pull the issue list

Two reports, run separately so fatals and non-fatals stay distinguishable:

```json
[
  { "name": "crashlytics_get_report",
    "arguments": { "appId": "1:636588470688:android:c27a676c8deb5eb0", "report": "topIssues", "pageSize": 25,
      "filter": { "versionDisplayNames": ["4.0.0 (40000)"], "issueErrorTypes": ["FATAL"] } } },
  { "name": "crashlytics_get_report",
    "arguments": { "appId": "1:636588470688:android:c27a676c8deb5eb0", "report": "topIssues", "pageSize": 25,
      "filter": { "versionDisplayNames": ["4.0.0 (40000)"], "issueErrorTypes": ["NON_FATAL"] } } }
]
```

Also run `ANR` unless the user narrowed the scope — an ANR is a production defect like any other.

The default window is the last 7 days. For a wider one pass both `intervalStartTime` and
`intervalEndTime` (max 90 days). Skip issues whose `state` is not `OPEN` — closed/muted issues were
already judged by a human.

Each group gives you `issue.id`, `title`, `subtitle`, `errorType`, `state`, `signals`,
`firstSeenVersion`, `lastSeenVersion`, a console `uri`, a `sampleEvent`, and per-window
`eventsCount` / `impactedUsersCount`.

## Step 3 — Skip anything already tracked

Check **both** directions before filing. An issue is already tracked if either is true:

1. A Crashlytics note references a GitHub issue —
   `crashlytics_list_notes` with the `issueId`, look for `github.com/MartinStyk/apk-analyzer/issues/`.
2. A GitHub issue references the Crashlytics issue id —
   `gh issue list --state all --limit 200 --search "<crashlytics-issue-id>"`.

The Crashlytics issue id is the durable key. Never dedupe on the title: Crashlytics titles are
derived from the top frame and change when the code moves.

When an issue is already tracked but the *note* is missing (i.e. only direction 2 matched),
backfill the note — that keeps the console usable for whoever looks there first.

## Step 4 — Decide whether it deserves an issue

| Kind | Rule |
|---|---|
| `FATAL` | Always file. A crash in a shipped release is always worth tracking. |
| `ANR` | Always file. |
| `NON_FATAL` | Judgement call — see below. |

Non-fatals split three ways, and **all three get an issue**; only the framing differs:

* **Worth fixing** — a real defect the app swallowed (a caught exception that leaves a broken or
  empty screen, a failed parse of a valid APK, a permission path that silently no-ops). File it as a
  normal bug.
* **Wrong / misreported** — the code reports a non-fatal for something that isn't actually a fault
  (an expected `SecurityException` from a restricted package, a user-cancelled operation, an
  expected null on an OS version). File it, and make the issue about **removing or downgrading the
  report** — a noisy non-fatal hides the real ones. Say plainly in the body that the recommendation
  is to stop reporting it, and why.
* **Unnecessary / not actionable** — fires only on a rooted device, an ancient OEM ROM, an OS bug
  with no app-side remedy. File it recommending the report be dropped or the condition handled
  quietly, and state what evidence led there (use the top devices / top OS reports).

Never silently discard a non-fatal. If it isn't worth fixing, that judgement *is* the issue.

## Step 5 — Gather detail before writing the issue

An issue with only a title and a link is not worth filing. For each one, pull:

```json
[
  { "name": "crashlytics_get_issue", "arguments": { "appId": "...", "issueId": "<id>" } },
  { "name": "crashlytics_batch_get_events", "arguments": { "appId": "...", "names": ["<sampleEvent>"] } },
  { "name": "crashlytics_get_report",
    "arguments": { "appId": "...", "report": "topAndroidDevices", "pageSize": 5, "filter": { "issueId": "<id>" } } },
  { "name": "crashlytics_get_report",
    "arguments": { "appId": "...", "report": "topOperatingSystems", "pageSize": 5, "filter": { "issueId": "<id>" } } }
]
```

Then map the top stack frame onto this repo. The frames are package-qualified
(`sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory.SearchHistoryDao_Impl`), so the
owning module follows directly from the package — see the module/package mapping in `AGENTS.md`.
Read the actual source of the top app frame before writing the issue; a triage issue that names the
wrong file wastes more time than no issue.

Ignore framework-only frames when locating the owner — find the deepest
`sk.styk.martin.apkanalyzer.*` frame.

## Step 6 — File the GitHub issue

Use the `create_issue` tool (not `gh issue create`) so the user gets the confirmation card. Label
it `bug`. If the crash is clearly scoped to one feature module and a matching scoped label exists
(`gh label list`), add that too — but don't invent labels.

Title: `<Exception type> in <the thing the user was doing>` — concrete and human, e.g.
`SQLiteException when recording a recently viewed app`. Not the raw Crashlytics title, which is a
class name.

Body:

```markdown
## What happens
<Plain-language description of what a user experiences. No class names in this paragraph.>

## Impact
- Version: 4.0.0 (40000)  •  first seen: <firstSeenVersion>
- <N> events, <M> impacted users (last 7 days)
- Top devices: <...>  •  Top OS: <...>

## Exception
```
<exception type and message, then the trimmed stack — app frames plus enough framework
context to be meaningful>
```

## Where it comes from
<The owning module and file, and what the code is doing at that frame.>

## Suspected cause
<Only if the evidence supports one. If it doesn't, write "Not yet determined" — a wrong
guess in a triage issue is worse than none.>

---
Crashlytics issue `<crashlytics-issue-id>`
<console uri>
```

Keep the "What happens" section in the user-facing voice described in `AGENTS.md` — active, present
tense, no internal identifiers.

For a **non-fatal you judged wrong or unnecessary**, replace "Suspected cause" with a
`## Recommendation` section that states whether to remove the report, downgrade it, or handle the
condition quietly, and why.

## Step 7 — Link back to Crashlytics

Immediately after the issue is created, write the note so the next triage run skips it:

```json
[
  { "name": "crashlytics_create_note",
    "arguments": { "appId": "...", "issueId": "<id>",
      "note": "Tracked in https://github.com/MartinStyk/apk-analyzer/issues/<number>" } }
]
```

A filed issue without its note is an incomplete triage — the next run will file a duplicate.

## Step 8 — Report

Summarise as a table: Crashlytics id (short), kind, events/users, the decision (filed / already
tracked / recommended-for-removal), and the GitHub issue number. Call out anything you deliberately
did not file and why.

Never mark a Crashlytics issue closed with `crashlytics_update_issue` during triage — closing it is
the `fix-crash` skill's job, after the fix actually ships.

## Verification

- [ ] Latest version came from git tags, not the `topVersions` ordering
- [ ] That version display name was confirmed present in Crashlytics
- [ ] Fatals, non-fatals, and ANRs were all queried
- [ ] Every issue was checked against both Crashlytics notes and GitHub search before filing
- [ ] Each non-fatal got an explicit fix / wrong / unnecessary judgement, and an issue either way
- [ ] Every filed issue names a real file in this repo, verified by reading it
- [ ] Every filed issue carries the Crashlytics issue id and console link
- [ ] Every filed issue has a matching Crashlytics note pointing back at it
- [ ] No Crashlytics issue state was changed
