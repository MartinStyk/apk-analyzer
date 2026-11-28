# Apk Analyzer Pro — paywall, Pro marks and support prompt

**Roadmap:** [EN-04 … EN-08](../roadmap.md#en--entitlement--billing), [HI-19](../roadmap.md#hi--snapshot--history-pillar-1--what-changed) · R1
**Status:** Implemented
**Scope:** the Pro sheet opened from What Changed, Export and Settings; the gold Pro mark and colour;
locked rows and the unlock card in What Changed; the Pro cues in the export flow; the Settings Pro
row; and a support prompt shown once after an update. Deliberately out: TV (out of Pro v1), price
experiments, a "Supporter since" date, and any change to which data is free.

Design canvas (row A is the approved design; rows B and C are the rejected alternatives):
<https://claude.ai/artifact/9CjYu76peXxfdk76kke2DJ>. Board names below (A1, A4b…) refer to it.

## Why

The sheet that ships today has no picture and no proof: a title, a count, two check lines and a
pale yellow button that reads as disabled. Locked rows in What Changed are grey blurred bars, so a
free user cannot tell whether a permission was added to their banking app or a file got 200 KB
bigger. The Pro gold sits next to warning orange and the terracotta attention colour and disappears.
Nothing in the app ever tells a long-time user that Pro exists or why it is worth paying for.

## Audience

Engineers, power users and security-minded people (~29k MAU). They read numbers, distrust hype and
punish nagging with one-star reviews. Every claim on a Pro surface is a counted fact from their own
phone, and every surface keeps a visible free way out.

## Design

**One offer, one colour, one mark.**

- **Colour — Marigold.** Light: fill `#F4B400`, text on fill `#231A00`, Pro text `#8A5A00`, soft
  container `#FFF4D6`. Dark: fill and Pro text `#FFC53D`, text on fill `#2A1E00`, soft container
  `#3A2C08`. Warning moves from `#FF9500` to `#E8590C` so the two stop looking alike.
- **Mark — gold coin.** A Marigold disc with a dark star cut in (`ProMark`). On a Marigold button it
  inverts: a dark disc with a gold star. The star alone carried no contrast on white, which is why
  today's star needs an outline.
- **Never colour alone.** Pro is always the coin plus the word "Pro". Pro gold is never used on
  status chips or warnings.

**The sheet sells with the user's own data.** The picture at the top is their real locked content
(a locked change card, or a preview of their export). The headline is a count. The benefit lines are
counts by category, or what the user can do with the file. No superlatives, no urgency, no timers.

**Counting rule.** What Changed counts **apps**: one row per app, summarising its changes over the
selected range. Free: the 5 most recent apps plus every app whose signing certificate changed. Pro:
the rest. The paywall uses the **All time** range. The cross-app log counts **changes** (the first
10 entries are free). The sheet, the in-list card and the bar all get their numbers from the same
predicate that locks the rows, so they can never disagree.

**Entry points.** The sheet opens from: a locked row, the What Changed card or bar, a Pro badge or
Save/Share in Export, the Settings Pro row, and the after-update prompt. Nothing else opens it.

**The after-update prompt is the only screen the user did not ask for.** It shows at most twice
ever, under the conditions in [After-update prompt](#a3--after-update-prompt). This supersedes
decision 3B in [business-model.md](../business-model.md) and the "never on launch" rule in
[`feature/pro/AGENTS.md`](../../../../feature/pro/AGENTS.md), which step 7 rewrites.

**Copy, used everywhere:**

| Where | Text |
|---|---|
| Support row (A1, A2, A3) | **Built open. Supported by you.** / Martin, author of Apk Analyzer |
| Buy button | Unlock lifetime Pro · {Play price} |
| A3 buy button | Support and get lifetime Pro · {Play price} |
| Caption | One payment. No subscription. |
| Restore | Restore purchase |

The price is always Play's formatted price. Only the button and caption mention money.

## Screens

### A1 — Sheet opened from What Changed

```
        ───                                  drag handle, no close button
 ┌─────────────────────────────────┐
 │  Strava 380 · Android versions  │         picture: the user's latest
 │ ┌─────────────────────────────┐ │         locked change, counts only
 │ │ (S) Signal  7.23 → 7.24  Pro│ │
 │ │  + 2 permissions added      │ │
 │ │  − 1 component removed      │ │
 │ └─────────────────────────────┘ │
 └─────────────────────────────────┘
 214 changes in 61 apps since 14 Oct
 6 apps are free to read. Pro opens the other 55.
 ⛨ 38 permissions added to your apps, and which apps got them
 ▦ 112 component changes, such as new services and receivers
 ▣ 54 size changes, update by update
 ⤓ Plus: export every detail of all 142 apps            (grey)
 ───────────────────────────────────
 (photo) Built open. Supported by you.
         Martin, author of Apk Analyzer
 [ ◉ Unlock lifetime Pro · €3.99 ]
   One payment. No subscription.
          Restore purchase
```

### A2 — Sheet opened from Export

Same frame as A1. Picture: a preview of the user's export file with their real app names; the Pro
columns are Marigold-headed. Headline "Add every detail to your export", line "The 8 Basics are
free. Pro adds the other 49." Benefit lines: **Audit permissions** of all 142 apps in one
spreadsheet; **Check every signing certificate** with SHA-256, subject and schemes; **Count
components per app**, from activities to intent filters; grey "Plus: read all 214 changes recorded
on this phone". Under the caption, two text buttons: **Export basics only** and **Restore
purchase**. After a purchase the user taps Save or Share again, as today.

### A3 — After-update prompt

A bottom sheet over the main tab. "Still free." (44sp bold) is the first line. Then "Everything
that was free stays free. Two new features are part of Pro:", two soft-gold rows (**App history** ·
214 changes recorded; **Inventory export** · every detail), the support row, the buy button with the
caption, and a **Continue for free** text button. Settings opens this same content.

Show only when **all** of these hold:

- the app version is newer than the version the prompt last showed in;
- an accomplishment happened in this session: back from app detail after a qualified session (the
  `ReviewEligibilityTracker` counter), or a finished Basics export;
- installed at least 7 days ago; entitlements refreshed first and Pro not owned;
- Play Billing is reachable and returns a price; phone only; not opened from an external APK;
- no paywall shown this session, and the review prompt not shown in the last 14 days;
- shown fewer than 2 times ever.

Show it on the first return to the main tab, never over cold start. Swiping it away counts as
"Continue for free". The history count is computed in the background; the prompt waits until it is
ready rather than showing a spinner.

### A4 / A4b — What Changed

- The 5 free rows, then **one card**: coin, "See what changed in 55 more apps", "Every added
  permission, component and size change. Certificate changes stay free.", **Unlock**.
- Below the card the list continues. Locked rows show the **app icon and name**; only the detail
  line is blurred, with a small "◉ Pro" label. Certificate-change rows stay fully readable.
  Tapping a locked row opens the sheet; long-press still copies.
- **A4b:** once the card has scrolled off the top, a bar appears above the tab bar ("See what
  changed in 55 more apps" / "Permissions, components, sizes" / **Unlock**) and stays while locked
  rows are on screen. The card and the bar are never visible together, and the bar never covers
  free rows.
- The log screen uses the same row and card with change counts.

### A5 / A5b — Export

The three steps stay. Changes:

1. The paid presets (Security audit, Permissions and components, Everything) carry the coin.
2. One line under the presets: "The 8 Basics are free. The other 49 details are Pro. You can still
   pick them and see them in the preview."
3. Rows keep today's tappable "Pro" label, now with the coin.
4. The Export-step notice sits directly under the Details card and names what needs Pro: "1 of these
   details needs Pro" / "Certificate SHA-256. The 8 Basics export for free." with **Unlock Pro** and
   **Export Basics instead**.
5. Save and Share carry the coin while the export needs Pro.

### A6 — Settings

Without Pro: a row "◉ Get Apk Analyzer Pro" / "A one-time purchase that supports development and
unlocks the newest features.", opening the sheet with the A3 content. With Pro: "Pro is active" /
"Thank you for supporting Apk Analyzer." Restore purchase stays reachable. The Settings row is shown only
where Google Play offers Pro, so release builds on the placeholder entitlements never see it.

### A8 — Purchase states

Price loading: content visible, button area is a skeleton. Pending: "Waiting for your payment" with
"Google Play is finishing the payment. Pro unlocks on its own when it goes through." Failed or
offline: "Couldn't reach Google Play. Nothing was charged." and the button reads "Try again".
Already owned: the sheet closes and the origin unlocks. No Google Play: "Purchases need Google Play.
Everything free keeps working.", no buy button.

## Implementation steps

1. **Design system** (`core:design-system`). Marigold values in `ApkAnalyzerColorPalette` (light and
   dark; add `proFill`/`onProFill` next to `pro`/`proContainer`), warning `#E8590C`. New `ProMark`
   component (disc + existing star icon, inverted variant), used by `ProBadge`, the Pro `InfoCard`
   tone and the Pro button colours. Verify: render the `ProBadge`, `InfoCard` and `Button` previews,
   light and dark.
2. **Shared counting rule** (`core:app-history`). Move the free/locked predicate out of
   `WhatChangedAccessPolicy` and add a summary over `AppHistoryDeviceTimeline.Available`: changed
   apps, total changes, free and locked app counts, recording start, counts per category, and the
   latest locked change. What Changed, the sheet and the after-update prompt all read it. This is
   the step with unit tests: free count includes certificate-change apps; counts exclude
   uncomparable snapshots; card and rows agree.
3. **The sheet** (`feature:pro`). Rebuild `ProScreen` as A1/A2 plus the A3 content.
   `PaywallReason.WhatChanged` carries the summary from step 2; `Export` adds the sample app names;
   new `Settings` and `Launch` reasons. Bundle the author photo as a drawable (never loaded from the
   network). Add the "Export basics only" result as the one exception to "the sheet returns
   nothing": a navigation result the export screen reads to select the Basics preset. Analytics:
   the new sources, plus card versus bar versus row in What Changed. Verify: previews per reason and
   per A8 state, light and dark.
4. **What Changed** (`feature:what-changed`). `LockableRow.Locked` carries the app name, icon
   reference and change count instead of only a key. New locked row (A4), the in-list card, and the
   bar driven by whether the card is visible in the `LazyListState`. The log screen uses change
   counts. Verify: previews; manual scroll on device.
5. **Export** (`feature:app-export`). Coin on presets and rows, the rule line, the reworded notice,
   the coin on Save and Share. Verify: previews of both steps.
6. **Settings** (`feature:settings`). The Pro row in both states.
7. **After-update prompt.** `SupportPromptPolicy` and `SupportPromptScheduler` in `feature:pro`
   (`core:common` keeps only the `engagement/` accomplishment signal, since feature policy does not
   belong there), with persisted keys for last shown version and times shown, and the conditions in
   [A3](#a3--after-update-prompt). The app shell collects the scheduler and navigates to
   `ProNavKey(PaywallReason.Launch)` once the user is on a main tab. The policy lives in a phone
   module, so it has no unit tests under the repo's test rules. Rewrite the "open only from an
   explicit tap" rule in `feature/pro/AGENTS.md` and decision 3B.
8. **Strings.** All copy above into `strings.xml` of the owning modules, then the 19 locales with
   the `translate-strings` skill.

## Deferred

- **"Supporter since {date}"** in Settings: needs Play's `purchaseTime` in `EntitlementsRepository`.
- **A thank-you snackbar after purchase**: needs a shared snackbar host; the sheet closing and the
  content unlocking is the confirmation for now.
- **Dark-mode boards** for A1–A6: drawn only as the colour spec (A7); checked in previews instead.
- **Auto-continuing an export after purchase:** considered and declined; the user taps Save again.
- **Permanent Pro entry on the Apps list** and a **tab badge** (option B): rejected as nagging.

## Open questions

None. Settled on 2026-11-28: option A; Marigold and the gold coin; warning moves; the after-update
prompt is approved; Settings reuses the A3 content; "Export basics only" is an allowed sheet result;
the owned state is "Pro is active"; What Changed counts use All time.
