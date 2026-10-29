---
name: release-app
description: Use to cut a release of ApkAnalyzer — pick the MAJOR.MINOR.PATCH version, draft release notes, get them approved, create the annotated tag that builds and uploads both the phone and the Android TV app to their beta tracks, and promote the phone app, the TV app, or both to production. Triggered by phrases like "release the app", "cut a release", "ship 3.6.0", "release the TV app", "tag a release", "make a new version", "release notes for the next version", "publish to the Play Store", "promote the release to production".
---

# Skill: Cut a Release

One annotated `MAJOR.MINOR.PATCH` tag releases both apps.
[`release.yml`](../../../.github/workflows/release.yml) builds the phone and the TV app, uploads the
phone bundle to `beta` and the TV bundle to `tv:Beta TV`, and attaches both APKs to one GitHub
release. Which app reaches production is decided afterwards, by running `Promote release to
production` with `phone`, `tv`, or `both`.

The tag is the entire release input. It supplies the version name, both version codes, and the
release notes, so **the tag must be annotated and its annotation must never be empty** — the
workflow fails its first job on a lightweight tag, and a tag that already reached GitHub is painful
to replace.

Nothing in the repository records the version. `gradle.properties` stays at `version.name=dev`;
there is no version-bump commit to make.

## Step 1 — Pick the version

The tag is exactly `MAJOR.MINOR.PATCH`. No `v` prefix, nothing else. A tag of any other shape
doesn't match the workflow trigger and silently releases nothing.

```bash
git fetch --tags
git tag --list --sort=-v:refname | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | head -5
```

Propose the next version from the previous tag and let the user correct it: patch for fixes only,
minor for new user-visible behaviour, major for a break in how the app is used. The version must
sort above the previous tag — Play rejects a lower version code. The TV version code carries a fixed
offset ([`version-code.sh`](../../../.github/scripts/version-code.sh)), so the two apps never
collide.

## Step 2 — Confirm what is being released

```bash
git status --short
git log --oneline -5 origin/develop
```

Release from `develop` at a commit that is pushed, clean, and green in CI. If CI for that commit
failed or is still running, say so and stop — the release workflow re-runs the same gates and will
fail the same way after the tag is public.

## Step 3 — Draft the release notes

Draft the notes from the changes since the previous tag, and work out whether each app has anything
its users can observe:

```bash
git log --oneline <previous-tag>..origin/develop
git log --oneline <previous-tag>..origin/develop -- tv 'feature/*/tv' 'feature/*/api' core ':!core/design-system/phone'
```

The TV path filter is a starting point, not the answer: a commit in a shared `core` module may
change nothing a TV user can see. Keep only what is observable on TV.

Write one set of notes with a section per app that changed, for example:

```
Phone
- ...

TV
- ...
```

Leave out the section of an app with no user-visible change, and drop the headings if only one app
changed. Record which apps changed — that decides what gets promoted in Step 7.

Turn the log into notes for users of the app, not for contributors:

* Follow the User-Facing Copy rules in [`AGENTS.md`](../../../AGENTS.md) — active voice, present
  tense, sentence case, name the concrete thing.
* Group into short bullets: new features first, then improvements, then fixes.
* Drop anything a user cannot observe — refactors, dependency bumps, CI and agent-context changes.
* Never surface class names, module names, PR numbers, or Android API names.
* Keep it short. The same text becomes the GitHub release body and is read on a phone or a TV.

## Step 4 — Get approval, always

**Never create a tag from a draft the user has not approved.** Print the proposed tag, its full
notes verbatim, and which apps you plan to promote as plain text in the response itself — not only
inside a question tool's option text, where the user may not see it — then ask for approval or
edits. Repeat until the user approves. If the user supplies their own notes, use them as given
rather than rewriting them.

Approving the notes also approves creating and pushing the tag from them — steps 5 and 6 follow
without a separate confirmation. It does not approve promotion to production.

## Step 5 — Create the annotated tag

Write the approved notes to a scratch file outside the repository and tag from it, so multi-line
notes survive intact:

```bash
git tag -a 3.6.0 -F /tmp/release-notes.txt <commit-sha>
```

Use `-a` (or `-s`). Never a bare `git tag <name>`, and never an empty or placeholder `-m`.

Verify the tag before pushing — both checks must pass:

```bash
git cat-file -t 3.6.0                                    # must print: tag
git for-each-ref --format='%(contents)' refs/tags/3.6.0  # must print the approved notes
```

If either check disagrees, delete the tag locally (`git tag -d <tag>`) and redo this step.

## Step 6 — Push the tag

Pushing starts the release. The approval gate is Step 4 (notes) and Step 5 (tag verification) —
once both are done, push:

```bash
git push origin 3.6.0
```

If a bad tag was already pushed, do not force-move it. Delete it on the remote, delete the GitHub
release it created, then push a corrected tag — or release the fix as the next patch version.

## Step 7 — Watch the run and promote

The release run uploads to both beta tracks, never production. Follow it with the
`analyze-ci-failure` skill if a job goes red; the phone and TV legs fail independently. The first
TV production release also goes through Play's Android TV quality review, which can take longer or
come back with requested changes.

Production is a separate, deliberate step: run `Promote release to production` manually with the
version, the form factor (`phone`, `tv`, or `both` — only the apps that changed in Step 3), and a
rollout percentage. Ask before running it.

Each release replaces the previous build on both beta tracks, so a version can only be promoted
while it is the latest one there. If an app skipped a few releases and later changes, promote the
newest version for it — it contains everything earlier.

Full pipeline reference, including secrets and the promotion rules:
[`ci-and-release.md`](../../../docs/engineering/ci-and-release.md).
