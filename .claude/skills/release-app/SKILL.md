---
name: release-app
description: Use to cut a release of ApkAnalyzer — pick the MAJOR.MINOR.PATCH version, draft release notes, get them approved, and create the annotated tag that drives the release pipeline. Triggered by phrases like "release the app", "cut a release", "ship 3.6.0", "tag a release", "make a new version", "release notes for the next version", "publish to the Play Store", "promote the release to production".
---

# Skill: Cut a Release

The tag is the entire release input. It supplies the version name, the version code, and the
release notes, so **the tag must be annotated and its annotation must never be empty** —
[`release.yml`](../../../.github/workflows/release.yml) fails its first job on a lightweight tag,
and a tag that already reached GitHub is painful to replace.

Nothing in the repository records the version. `gradle.properties` stays at `version.name=dev`;
there is no version-bump commit to make.

## Step 1 — Pick the version

The tag is exactly `MAJOR.MINOR.PATCH` — three numbers, no `v` prefix, no suffix. Anything else
does not match the workflow trigger and silently releases nothing.

```bash
git fetch --tags
git tag --sort=-v:refname | head -5
```

Propose the next version from the change set and let the user correct it: patch for fixes only,
minor for new user-visible behaviour, major for a break in how the app is used. `versionCode` is
derived as `MAJOR * 10000 + MINOR * 100 + PATCH`, so the new tag must sort above the last one under
that formula — Play rejects a lower code.

## Step 2 — Confirm what is being released

```bash
git status --short
git log --oneline origin/develop -5
```

Release from `develop` at a commit that is pushed, clean, and green in CI. If CI for that commit
failed or is still running, say so and stop — the release workflow re-runs the same gates and will
fail the same way after the tag is public.

## Step 3 — Draft the release notes

```bash
git log --oneline <previous-tag>..origin/develop
```

Turn the log into notes for users of the app, not for contributors:

* Follow the User-Facing Copy rules in [`AGENTS.md`](../../../AGENTS.md) — active voice, present
  tense, sentence case, name the concrete thing.
* Group into short bullets: new features first, then improvements, then fixes.
* Drop anything a user cannot observe — refactors, dependency bumps, CI and agent-context changes.
* Never surface class names, module names, PR numbers, or Android API names.
* Keep it short. The same text becomes the GitHub release body and is read on a phone.

## Step 4 — Get approval, always

**Never create the tag from a draft the user has not approved.** Show the proposed version and the
full notes verbatim, and ask for approval or edits. Repeat until the user approves. If the user
supplies their own notes, use them as given rather than rewriting them.

## Step 5 — Create the annotated tag

Write the approved notes to a scratch file outside the repository and tag from it, so multi-line
notes survive intact:

```bash
git tag -a 3.6.0 -F /tmp/release-notes.txt <commit-sha>
```

Use `-a` (or `-s`). Never `git tag 3.6.0` on its own, and never an empty or placeholder `-m`.

Verify before pushing — both checks must pass:

```bash
git cat-file -t 3.6.0                                    # must print: tag
git for-each-ref --format='%(contents)' refs/tags/3.6.0  # must print the approved notes
```

If either check disagrees, delete the tag locally (`git tag -d 3.6.0`) and redo this step.

## Step 6 — Push the tag

Pushing starts the release. An agent cannot push, so hand the exact command to the user:

```bash
git push origin 3.6.0
```

If a bad tag was already pushed, do not force-move it. Delete it on the remote, delete the GitHub
release it created, then push a corrected tag — or release the fix as the next patch version.

## Step 7 — Watch the run and promote

The release run publishes to the Play **beta** track, never production. Follow it with the
`analyze-ci-failure` skill if a job goes red.

Production is a separate, deliberate step: the `Promote release to production` workflow, run
manually with the version and a rollout percentage. It promotes whatever currently sits on beta, so
check the beta track first if the release run finished a while ago.

Full pipeline reference, including secrets and the promotion rules:
[`ci-and-release.md`](../../../docs/engineering/ci-and-release.md).
