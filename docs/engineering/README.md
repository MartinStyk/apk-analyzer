# Engineering Documentation

How this repository is built, and the rules a change is held to. Product scope and feature design
live in [`docs/product/`](../product/README.md); one-off decisions and audits live in
[`docs/technical/`](../technical/README.md).

| Document | Read it when |
|---|---|
| [Architecture](architecture.md) | You need the module graph, the dependency rules, or the ViewModel/data-layer/design-system shapes |
| [Coding standards](coding-standards.md) | Before writing code — conventions, state handling, and the reasoning behind them |
| [Verification](verification.md) | You want to know what Spotless, Detekt, Lint and LeakCanary actually check, and what CI gates on |
| [AI workflow](ai-workflow.md) | You're working with a coding agent, or writing/updating an `AGENTS.md` or a skill |

The canonical, terse version of all of this is [`AGENTS.md`](../../AGENTS.md) — the file both humans
and agents are expected to read first. These documents expand it with reasoning and worked examples
from the codebase; where they disagree, `AGENTS.md` wins.

New here? Start with [`CONTRIBUTING.md`](../../CONTRIBUTING.md) for the workflow, then
[architecture](architecture.md) and [coding standards](coding-standards.md).
