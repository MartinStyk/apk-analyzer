# APK Analyzer — Shipped & Retired

Terse reference for stable IDs that are **Done** or **Retired**. Active, open work lives in
[roadmap.md](roadmap.md) — this file exists so those IDs still resolve when something still open
references them as a prerequisite, without cluttering the roadmap with finished work. No notes
beyond a one-line item name; for the reasoning behind a decision, check the item's module
`AGENTS.md`, its feature doc under `features/`, or `git log` on `roadmap.md`.

## 1.1 Inventory & Browse

FR-01 Installed app list · FR-02 Search with history · FR-03 Advanced filter sheet · FR-04 Quick
filter chips · FR-05 Sorting · FR-06 Recently viewed apps · FR-07 Install source filter

## 1.1b Browse by Attribute

CE-01 Attribute index infrastructure · CE-05 Browse screen · FR-08 Browse dimension: permission ·
FR-09 Browse dimension: signing certificate · FR-40 Browse dimension: target SDK / min SDK ·
FR-41 Browse dimension: install source · FR-42 Browse dimension: shared UID · FR-43 Browse
dimension: app category

**Module consolidation.** `feature:permissions` and `feature:statistics` — both placeholder screens
each holding a top-level nav slot — were deleted and collapsed into `feature:browse`, now fully
implemented per `CE-05`. That freed the bottom-nav slot Pillar 1's What Changed tab uses in R1.

## 1.2 App Detail

FR-10 Detail overview + badges · FR-11 General info screen · FR-12 Permissions view (per app) ·
FR-13 Components views · FR-14 Certificate detail view · FR-15 Features (uses-feature) view ·
FR-16 Manifest viewer

## 1.3 APK File Analysis

FR-19 Open `.apk` from another app · FR-20 Pick an `.apk` from storage

FR-21 Recent APK files — **Retired.** Picked files are temp copies released after use, and the
original source can be deleted or moved independently, so a "recent" list would routinely point at
files that no longer exist.

## 1.4 Device Statistics — Retired

FR-22 Device statistics screen, FR-23 Chart → filtered list jumps — both **Retired**, superseded by
Browse by Attribute (§1.1b): a chart of "how many apps target API 30" is just the bucket counts of
the target-SDK index, and jumping from a chart to a filtered list is what an attribute index does
natively once statistics and browse stopped being separate screens.

## 1.5 Export & Share

FR-24 APK export / share · FR-25 Icon export

## 1.6 App & UI

FR-28 Theme / color scheme setting · FR-29 Settings screen · FR-30 Usage-access permission flow

## 1.8 Data Gaps

FR-32 Manifest security flags · FR-34 Split APKs / config splits · FR-35 Shared UID group ·
FR-39 App category · EX-07 Component intent filters · EX-08 Content-provider path permissions
