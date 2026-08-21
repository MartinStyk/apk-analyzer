---
name: translate-strings
description: Use when adding a new user-facing string, changing existing English copy, or adding/auditing a locale's strings.xml translations. Triggered by phrases like "translate strings", "add translation", "update translations", "localize this screen", "add a new language", "sync locale strings", "translate to <language>".
---

# Translate Strings

Translating a string here is not a lookup — it is writing the same UX copy in another language. Read
the string's usage context before translating it, and match the tone rules in the root
[`AGENTS.md`](../../../AGENTS.md#user-facing-copy) (active voice, present tense, sentence case, name
the concrete thing) in the target language's own idiom, not a literal transliteration of the English
sentence structure.

## Supported Languages

| Language | Folder qualifier | Plural categories needed | Notes |
|---|---|---|---|
| Spanish | `values-es` | one, other | |
| German | `values-de` | one, other | |
| French | `values-fr` | one, other | `many` exists in CLDR (decimal/compact numbers) but never triggers for the integer counts this app uses — skip it |
| Portuguese (Brazil) | `values-pt-rBR` | one, other | Do not use plain `values-pt` — Portugal Portuguese differs |
| Italian | `values-it` | one, other | `many` same as French — skip |
| Japanese | `values-ja` | other | No grammatical plural; only `quantity="other"` is ever selected |
| Chinese (Simplified) | `values-zh-rCN` | other | |
| Korean | `values-ko` | other | |
| Vietnamese | `values-vi` | other | |
| Indonesian | `values-in` | other | Android's legacy resource qualifier for Indonesian is `in`, not `id` |
| Russian | `values-ru` | one, few, many, other | Full Slavic set — integers routinely land in `few`/`many`, omitting them reads as broken grammar |
| Ukrainian | `values-uk` | one, few, many, other | Same shape as Russian |
| Polish | `values-pl` | one, few, many, other | Same shape as Russian |
| Slovak | `values-sk` | one, few, many, other | `few` = 2–4, `many` = has a fraction |
| Czech | `values-cs` | one, few, many, other | Same shape as Slovak |
| Turkish | `values-tr` | one, other | |
| Arabic | `values-ar` | zero, one, two, few, many, other | Full six-category set — 0/1/2 are their own categories and are common counts in this app. RTL mirroring is automatic; don't hand-adjust layout, just translate the text |
| Hindi | `values-hi` | one, other | CLDR quirk: `0` maps to `one` in Hindi, not `other` |

If asked to add a language outside this table, confirm the language and its folder qualifier with the
user before creating files — don't guess a BCP-47 tag.

## Control Language: Slovak (`values-sk`)

English (`values/strings.xml`) is still the canonical source for keys, placeholders, and structure —
but English UI copy is often terse enough to be ambiguous about the actually-intended nuance. Slovak
(`values-sk`) is this app's **control translation**: the app owner personally reviews and corrects it,
so a confirmed Slovak string encodes the verified intended meaning, tone, and terminology choice more
precisely than the English source alone.

When translating a string into any language other than Slovak, read both the English source and the
Slovak translation before writing the target-language text — use English for literal meaning and
structure, and Slovak to resolve tone, nuance, or ambiguity English leaves open. Where the two would
suggest different readings, follow the interpretation Slovak embodies, since it reflects reviewed
intent rather than a first-pass translation. If a Slovak entry doesn't exist yet for a key, fall back
to English alone and say so rather than guessing what a not-yet-written control would say.

Don't translate *from* Slovak's grammar — its declension and word order don't transfer to unrelated
languages. Slovak is a meaning/tone reference; English remains the structural source (placeholder
count and order, key names, sentence type).

## Android Platform Vocabulary

`Activity`, `Service`, `Receiver` (Broadcast Receiver), `Provider` (Content Provider), `Intent
filter`, `Permission`, `Manifest`, `Package`, `APK`, `SDK` are Android platform terms, not ordinary
English prose — this app's strings already render them as short category labels
(`components_type_activities` = "Activities", `components_scope_providers` = "Providers", etc., see
`feature/app-detail/impl/src/main/res/values/strings.xml`). The right call is neither "always
translate" nor "always keep English" — it's **use whichever term Android's own official localization
already uses for that exact word in that language**, so it matches what a user may already recognize
from Android Studio, developer.android.com, or the Play Console in their language:

- **Google officially localizes Android developer docs for**: Spanish, Japanese, Korean, Portuguese
  (Brazil), Russian, Chinese (Simplified/Traditional), Indonesian, Turkish, Vietnamese — largely
  overlapping with this table's `es`, `ja`, `ko`, `pt-rBR`, `ru`, `zh-rCN`, `in`, `tr`, `vi`. For
  these, use the exact term developer.android.com uses in that locale for `Activity`/`Service`/
  `Receiver`/`Provider`/etc. Verify the current term rather than recalling it from memory or an old
  session — Google's supported-docs-language list and its terminology both drift over time.
- **Not officially localized**: German, French, Italian, Polish, Slovak, Czech, Ukrainian, Arabic,
  Hindi. For these, the established local Android developer convention is almost always to keep the
  English term as a loanword (`Activity`, `Service`, `Broadcast Receiver`, `Content Provider`) — that
  also avoids inventing a translation no one in that market's Android ecosystem actually uses.
- `APK`, `Intent`, `Manifest` stay in English in effectively every language, including ones with
  localized docs — they function as fixed technical terms across the whole Android world, the same
  way this skill already treats `APK` elsewhere.
- If you're not sure for a specific language/term pair, say so and check developer.android.com in
  that locale (or ask) instead of guessing silently — getting this wrong reads as a translation that
  doesn't know the platform, which undermines trust more than an occasional English loanword would.

## Always-English Keys

A handful of specific keys are pinned to the exact English source text in **every** language,
Slovak included — not translated, not adapted. These are deliberate app-owner calls (not derivable
from the general platform-vocabulary rule above), so don't second-guess them into translation during
an audit:

- `permission_label_wake_lock` → `Wake lock`. The English source string itself reads "Keep Device
  Awake" (plain prose, not this term) — every locale still renders the Android API name `Wake lock`
  instead of translating that source sentence. This is an intentional exception to "translate the
  English source," not an oversight.
- `permission_label_ad_id` → `Advertising ID`, `permission_label_ad_attribution` → `Ad Attribution`,
  `permission_label_ad_custom_audience` → `Ad Audiences`, `permission_label_ad_topics` → `Ad Topics`
  (all `core/app-permissions`).
- `permission_label_bluetooth_advertise` → `Bluetooth Advertise` (`core/app-permissions`).
- `permissions_level_dangerous` / `_signature` / `_internal` / `_normal` (`feature/app-detail/impl`)
  and their `browse_protection_level_*` counterparts (`feature/browse/impl`) → `Dangerous` /
  `Signature` / `Internal` / `Normal`.
- `app_detail_target_sdk` and `general_info_target_sdk` (`feature/app-detail/impl`) → `Target SDK`.
  `sort_target_sdk` (`feature/apps/impl`) → `Target Android SDK` (matches that key's own English
  source, which includes "Android" where the other two don't — copy each key's own default text
  verbatim rather than harmonizing the three into one phrase).

If a new key looks like it belongs in one of these families (e.g. a future `permission_label_ad_*`
or another `*_target_sdk` label), ask before deciding whether it joins the always-English set or
gets a normal translation — don't extend the pattern by inference.

## Scope: One Module at a Time

Every module with a `values/strings.xml` gets its own `values-<qualifier>/strings.xml` with the exact
same key set. 

Translate module by module. Don't merge keys from different modules into one file, and don't create a
locale folder for a module that has no `values/strings.xml`.

## Order of Work: String-by-String vs Language-by-Language

For a small batch of **new or changed strings**, translate string-by-string across all languages, not
language-by-language across all strings. Reading the usage context (workflow step 3 below) is the
expensive, error-prone part — do it once per string, then produce every language's translation while
that context and its resolved meaning are still fresh. Translating language-by-language for a handful
of new keys means re-deriving the same context up to 18 times and risks resolving an ambiguous string
differently on different passes. Translate Slovak first (or immediately after confirming the English
meaning) among the batch, since it's the control language — having it settled gives you, and the user
on review, a second concrete reference before propagating to the remaining languages.

The exception is a **large-scale audit of many already-existing strings** (a full-module or full-app
correctness review, not a handful of new keys) — group that by language instead. A single sustained
pass through one language builds up internal terminology and register consistency across the whole
file that a string-by-string pass would fragment, and it parallelizes cleanly across languages when
multiple reviewers (human or agent) are available.

## Workflow

1. **Scope the change.** `git diff develop -- '**/values/strings.xml'` (or the equivalent for the
   branch you're on) to see which keys are new or whose English text changed. If this is a fresh
   language addition instead of an incremental update, the scope is every key in the module.
2. **Skip untouched keys in incremental updates.** If a `values-<lang>/strings.xml` already exists,
   only touch the keys that are new or whose source text changed. Don't regenerate an already-correct
   translation just because you're in the file — that's how translations silently drift from a
   reviewed state.
3. **Read the usage context before translating each string.** Grep for
   `R.string.<key>` / `R.plurals.<key>` in the module's Kotlin sources and open the Composable that
   uses it. You need to know:
   - What kind of string it is — screen title, button label, content description, error message,
     empty state, explanation body, technical field label.
   - What screen/feature it belongs to, so domain terms (permission, activity, signing certificate,
     split APK) get the term a tech-literate but non-developer reader in that language actually uses,
     not a dictionary-literal translation. For Android platform vocabulary specifically (Activity,
     Service, Provider, Permission, Manifest, APK, ...), see "Android Platform Vocabulary" below.
   - The meaning and order of any `%1$s` / `%2$d` placeholders — read the call site to see what each
     argument actually is.
   - If you're translating into a language other than Slovak, also read the corresponding
     `values-sk/strings.xml` entry when one exists — see "Control Language: Slovak" above. It often
     resolves ambiguity the English text alone leaves open.
4. **Translate.** Keep it natural in the target language, not a clause-for-clause mirror of the
   English sentence. Preserve:
   - Placeholder indices tied to their original argument meaning. You may reorder where `%1$s` and
     `%2$s` appear in the sentence to fit target-language word order, but `%1$s` must still refer to
     whatever the first Kotlin call argument is — never renumber based on translated word order.
   - XML escaping: apostrophes as `\'` (or wrap the whole string in `"..."`), `&` as `&amp;`, existing
     `<b>`/`<i>` spans if present.
   - Every `quantity=` category the target language needs per the table above, even though the English
     source only defines `one`/`other`. Write each category's own grammatically correct sentence —
     don't copy the `other` text into `few`/`many`/`two` untranslated.
   - `content_description_*` strings stay terse (they're for screen readers, not visible copy).
5. **`android_versions` string-array (`core/apps`):** keep the dessert-name codename in English for
   Latin/Cyrillic-script languages, matching the convention on the Play Store and in Android's own
   settings for that locale — most languages keep "Cupcake", "Donut", etc. untranslated. Only
   transliterate for CJK languages, matching the existing `values-ja` file's pattern. If unsure for a
   given language, check what Android's own Settings app uses for that locale rather than guessing.
6. **Check key parity** between the base file and the locale file you touched:
   ```bash
   base=core/apps/src/main/res/values/strings.xml
   target=core/apps/src/main/res/values-es/strings.xml
   diff <(grep -oE 'name="[a-zA-Z0-9_]+"' "$base" | sort -u) \
        <(grep -oE 'name="[a-zA-Z0-9_]+"' "$target" | sort -u)
   ```
   Any line in the diff is a missing or extra key in the target locale.
7. **Validate the XML is well-formed** (PowerShell, no extra tooling needed):
   ```powershell
   [xml] (Get-Content "core/apps/src/main/res/values-es/strings.xml" -Raw) | Out-Null
   ```
   Throws on malformed XML (unescaped `&`, unclosed tags, etc.).

## Explicitly Not This Skill's Job

- Don't touch the English `values/strings.xml` — this skill translates existing approved copy, it
  doesn't write new source strings. If the English string itself needs to change, that's a normal code
  change under the root `AGENTS.md` "User-Facing Copy" rules, done first, separately.
- Don't invent new string keys to make a translation easier — if a language genuinely needs a
  restructured sentence that the current key/placeholder shape can't express, flag it instead of
  reshaping the source string unilaterally.
- Per the root `AGENTS.md` verification table, don't run `lintDebug` or `spotlessCheck` after every
  file — `strings.xml` isn't Kotlin, spotless doesn't touch it. The XML well-formedness check above is
  the right-sized local check; CI's `lintDebug` catches anything deeper (missing plural fallback,
  malformed format strings).
