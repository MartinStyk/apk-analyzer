# GitHub Copilot Instructions

Treat [`AGENTS.md`](../AGENTS.md) as the canonical repository instructions. Read it before making
changes, then read the nearest module-level `AGENTS.md` for every path you modify. The closest
`AGENTS.md` takes precedence when instructions differ.

Task-specific workflows live in [`.claude/skills/`](../.claude/skills/). This directory uses the
Agent Skills standard and is shared by Claude and Copilot. Load the relevant skill before starting
a matching task.

Keep this file as a thin adapter. Do not duplicate architecture, conventions, module details, or
skill instructions here. Trust the canonical context and only search when it is incomplete or
outdated.
