---
name: git-commit-author
description: Use whenever creating a git commit in this repo. Commits must always be authored as the human user only — never as Claude, never with a Claude/AI co-author trailer. Triggered by any commit creation in this repository.
---

# Git Commit Author Policy

All commits in this repository are authored by the human user only.

* Do **not** add `Co-Authored-By: Claude ...` or any AI co-author trailer.
* Do **not** add a `Claude-Session: ...` link or similar footer.
* Do **not** pass `--author` to override identity — let the commit use the
  local `git config user.name` / `user.email` as-is (currently
  `Martin Styk <martin.styk@gmail.com>`).
* Commit messages contain only the actual change description (subject +
  optional body), nothing else.

Verify before committing: `git log -1 --format='%an <%ae>%n%B'` on recent
commits shows plain messages with a human author and no AI trailers — match
that style.
