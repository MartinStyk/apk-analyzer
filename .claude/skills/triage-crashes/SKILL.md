---
name: triage-crashes
description: Use to review production Crashlytics crashes and non-fatals for the latest release and file a GitHub issue for each one that isn't tracked yet. Triggered by phrases like "triage crashes", "check production crashes", "look at Crashlytics", "any new crashes", "review non-fatals", "file issues for the latest release crashes", "what's crashing in production".
---

# Skill: Triage Production Crashes and Non-Fatals

> Reads Crashlytics for the latest released version, decides which issues are worth tracking, and
> files one GitHub `bug` issue per untracked issue — linking both directions so the same crash is
> never filed twice. Not every non-fatal gets an issue: one that's already gracefully handled and
> still gives us useful signal just to watch gets a Crashlytics note recording that judgement,
> nothing more.

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
git tag --sort=-v:refname | Where-Object { $_ -match '^\d+\.\d+\.\d+$' } | Select-Object -First 1
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

## Step 3 — Skip anything already reviewed

Check **both** directions before doing anything else. An issue was already reviewed if either is
true:

1. A Crashlytics note records a prior triage decision — `crashlytics_list_notes` with the `issueId`.
   This covers two shapes: a note linking a GitHub issue (`github.com/MartinStyk/apk-analyzer/issues/`),
   and a note recording a **no-issue-needed** judgement from Step 5 (see below) with no GitHub link at
   all.
2. A GitHub issue references the Crashlytics issue id —
   `gh issue list --state all --limit 200 --search "<crashlytics-issue-id>"`.

The Crashlytics issue id is the durable key. Never dedupe on the title: Crashlytics titles are
derived from the top frame and change when the code moves. This only catches the *same* `issueId`
recurring (same stack signature) — a different occurrence of the same underlying condition (a
different package name, path, or byte count producing a fresh `issueId`) is not caught here and
reaches Step 5 again on its own merits. That's fine: Step 5's criteria are meant to be re-applied
consistently, not memorized in a lookup table, so it reaches the same conclusion each time.

When a GitHub issue exists but the *note* is missing (i.e. only direction 2 matched), backfill the
note — that keeps the console usable for whoever looks there first. Skip the rest of triage for this
issue either way.

## Step 4 — Investigate each issue in parallel

Gathering event/device/OS data, mapping the crash to source, and reaching a judgement is read-only
and independent per issue — spawn one forked subagent (`Agent` tool, `subagent_type: "fork"`) per
issue still standing after Step 3, all launched in the same message so they run in parallel. A fork
inherits this skill's instructions and the module/package mapping already in context, so the prompt
only needs the issue id and appId.

Each fork:

1. Pulls its issue's detail:

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

2. Maps the top stack frame onto this repo. The frames are package-qualified
   (`sk.styk.martin.apkanalyzer.core.userpreferences.searchhistory.SearchHistoryDao_Impl`), so the
   owning module follows directly from the package — see the module/package mapping in `AGENTS.md`.
   Ignore framework-only frames when locating the owner — find the deepest
   `sk.styk.martin.apkanalyzer.*` frame.
3. Reads the actual source of the top app frame before judging or writing anything. A triage issue
   that names the wrong file wastes more time than no issue, and a verdict reached without reading
   the code is just a guess.
4. Applies the judgement criteria in Step 5 and returns a **proposed** verdict — worth-fixing /
   useful-signal / noise-downgrade / not-actionable — with its reasoning, the owning file, and (for
   every verdict except useful-signal) a draft title/body per Step 6's template.

A fork only investigates and proposes. It never calls `crashlytics_create_note`,
`crashlytics_update_issue`, or files a GitHub issue — filing and notes happen in the coordinator
(Steps 6–7) after reconciliation, so one place sees every issue in the batch before anything is
written or filed.

## Step 5 — Reconcile proposed verdicts and decide

Each fork judges its issue against the criteria below.

| Kind | Rule |
|---|---|
| `FATAL` | Always file — unless it matches the third-party/no-app-frame shape under **Not
  actionable** below, which applies regardless of `errorType`. |
| `ANR` | Same as `FATAL` — always file unless it matches **Not actionable** below. |
| `NON_FATAL` | Judgement call — see below. |

A non-fatal reaching this step never gets silently discarded — every one gets an explicit judgement,
recorded via a Crashlytics note either way (Step 7). What varies is whether that judgement also
produces a GitHub issue:

* **Worth fixing** — a real defect the app swallowed (a caught exception that leaves a broken or
  empty screen, a failed parse of a valid APK, a permission path that silently no-ops). File it as a
  normal bug.
* **Working as designed, useful signal — no issue.** The flow already degrades gracefully (caught,
  no crash, a sensible fallback or error state), **and** knowing how often it happens is genuinely
  useful: the user ends up with something materially worse than intended (a whole feature
  unavailable, an action failing outright, a screen that won't load) rather than something cosmetic,
  and a rising or falling rate could plausibly justify a future decision (raising a limit further,
  pinning a dependency, prioritizing a real fix). There's nothing to *do* about it right now, so
  don't file — just record the judgement in a Crashlytics note (Step 7) and leave the code exactly as
  it is. Ask two questions to tell this apart from the next bucket:
  1. Is the degraded outcome something a user would actually notice as worse, not just an invisible
     fallback?
  2. Would the occurrence rate, if it climbed, ever change what gets built or fixed?

  Both "yes" → this bucket. Worked example: #216 (on-device AI model download fails from an SDK/
  coroutines version mismatch — the feature stays unavailable, no fix exists yet without a dependency
  decision, and the rate is worth watching in case that changes).
* **Noise / mis-reported — file, recommend downgrading.** The flow degrades gracefully too, but the
  degradation is cosmetic or so routine that per-occurrence tracking has no debugging value at all —
  the answer to both questions above is "no." Recording it as a non-fatal actively hides real signal
  behind volume. File an issue whose body says plainly that the recommendation is to downgrade or
  remove the report, and why. Worked example: #204/#205/#213/#214/#215 (an app icon silently falling
  back to a default — invisible to the user, and no plausible future decision depends on how often it
  happens; one call site had produced five separate issues before this rule existed).
* **Not actionable** — fires only on a rooted device, an ancient OEM ROM, an OS bug, or (for
  `FATAL`/`ANR`) a trace with zero `sk.styk.martin.apkanalyzer.*` frames and a blamed owner that isn't
  this app. Nothing in this repo can fix it. File an issue recommending the report be
  closed/muted, stating the evidence (top devices / top OS reports, or the absent app frame). Worked
  examples: #210 (third-party repackaging tool's injected `ComponentFactory`, crashes before any app
  code runs), #219 (background ANR, Crashlytics itself reports the root cause unknown, no app frame
  anywhere on the stack).

Before accepting any fork's proposed verdict, compare it against every other fork's from this run.
Two or more issues that trace back to the same underlying call site or root cause — even with
different `issueId`s, different package names, paths, or byte counts on the stack — must not become
separate GitHub issues. Fold them into one issue that lists every Crashlytics issue id it covers,
rather than filing one per fork. #204/#205/#213/#214/#215 is exactly the failure this check exists to
prevent: the same call site produced five separate issues before this reconciliation step existed.

Once reconciled, each issue (or merged group of issues) has a final verdict — this is what Steps 6–7
act on, not the fork's raw proposal.

## Step 6 — File the GitHub issue

Only for **worth fixing**, **noise / mis-reported**, and **not actionable** verdicts — a **working as
designed, useful signal** verdict skips this step entirely and goes straight to Step 7.

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

## Step 7 — Write the Crashlytics note

Every issue judged in Step 5 gets a note — filed or not. That note is what lets Step 3 recognize this
exact `issueId` on the next run without re-judging it. For an issue folded into a merged group during
reconciliation, write the note separately for **each** `issueId` in the group, all pointing at the
same GitHub issue number — Step 3 dedupes per `issueId`, so a merged sibling without its own note
looks unreviewed on the next run.

Filed:

```json
[
  { "name": "crashlytics_create_note",
    "arguments": { "appId": "...", "issueId": "<id>",
      "note": "Tracked in https://github.com/MartinStyk/apk-analyzer/issues/<number>" } }
]
```

Judged **working as designed, useful signal** (no issue filed) — record the judgement itself, not a
link, so it reads as a decision rather than a gap:

```json
[
  { "name": "crashlytics_create_note",
    "arguments": { "appId": "...", "issueId": "<id>",
      "note": "Reviewed: already handled gracefully, occurrence rate is worth watching, no code change and no GitHub issue needed. See triage-crashes SKILL.md, Step 5." } }
]
```

A judged issue without its note is an incomplete triage — the next run repeats the work, or worse,
files a duplicate.

## Step 8 — Report

Summarise as a table: Crashlytics id (short), kind, events/users, the decision (filed as worth-fixing
/ filed as noise-downgrade / filed as not-actionable / reviewed-no-issue / already reviewed), and the
GitHub issue number where one exists. Call out every **working as designed, useful signal**
judgement explicitly, with the two-question reasoning from Step 5 — that's the one disposition with
no GitHub issue to point at, so the report is the only place it's visible. Call out every group
merged during Step 5 reconciliation too, listing every Crashlytics issue id folded into it — that's
the other disposition a reader can't reconstruct from GitHub alone.

Never mark a Crashlytics issue closed with `crashlytics_update_issue` during triage — closing it is
the `fix-crash` skill's job, after the fix actually ships.

## Verification

- [ ] Latest version came from git tags, not the `topVersions` ordering
- [ ] That version display name was confirmed present in Crashlytics
- [ ] Fatals, non-fatals, and ANRs were all queried
- [ ] Every issue was checked against Crashlytics notes and GitHub search before being judged
- [ ] Every issue still standing after Step 3 was investigated by its own forked subagent, launched
      in parallel with the others, and no fork filed a GitHub issue or wrote a Crashlytics note
- [ ] Each non-fatal reaching Step 5 got an explicit proposed judgement — worth-fixing / useful-signal
      / noise / not-actionable — not a default
- [ ] A **useful-signal** verdict was reached by reading the actual code, not the title alone, and
      answers both Step 5 questions
- [ ] Every fork's proposed verdict was cross-checked against the others for a shared call site or
      root cause before filing, and any match was folded into one issue
- [ ] Every filed issue names a real file in this repo, verified by reading it
- [ ] Every filed issue carries the Crashlytics issue id and console link
- [ ] Every judged issue — filed or not — has a matching Crashlytics note, including every sibling in
      a merged group
- [ ] No Crashlytics issue state was changed
