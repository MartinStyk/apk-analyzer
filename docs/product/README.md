# Product Documentation

```
docs/product/
  roadmap.md          what we are building and in what order — the single source of scope
  features/           one design doc per feature, written before it is implemented
```

## The split

**`roadmap.md` owns scope and sequencing.** Every line item has a stable ID (`FR-12`, `HI-03`,
`RI-02`) and a status. It says *what* and *when*, never *how*.

**A feature doc owns design.** It says *how* one feature works and why it works that way —
interaction model, screen structure, edge cases, implementation steps. It covers a set of roadmap
IDs and never invents scope that the roadmap doesn't carry.

If a feature doc needs work the roadmap doesn't list, the roadmap gets a new ID first.

## Writing a feature doc

Name the file after the feature, not its roadmap position: `features/app-detail.md`. Section
numbers move — §1.1b was inserted mid-review and shifted everything under it — but IDs never do.
Traceability comes from the header block, which every feature doc starts with:

```markdown
# Feature Name — short description of the deliverable

**Roadmap:** [FR-12 … FR-18](../roadmap.md#12-app-detail) · R0
**Status:** Approved design, not yet implemented
**Scope:** one sentence on what is in, and what is deliberately out
```

Then, in whatever order the feature needs:

| Section              | Holds                                                                   |
|----------------------|--------------------------------------------------------------------------|
| Why                  | What the data or product gap is, in evidence terms                       |
| Audience             | Who this is for — decides how much explanation the UI carries            |
| Design               | The interaction idiom and the cross-cutting rules, before any screen     |
| Screens              | One block per screen, with an ASCII sketch                               |
| Implementation steps | Ordered, each naming the shared work it pulls in and why that step needs it |
| Deferred             | What was consciously left out, so it isn't re-litigated                  |
| Open questions       | Decisions still outstanding, each with the options                       |

Two conventions worth keeping, both from `features/app-detail.md`:

- **No speculative foundations step.** A shared component is created in the step that first needs
  it; a shared abstraction is extracted when the *second* consumer appears, not predicted at the
  first.
- **Deferred and Open questions are load-bearing.** They are what makes the doc survive a
  months-long gap between approval and implementation.

## Status

When a feature ships, update the roadmap status for its IDs and mark the feature doc's header
`Implemented`. Keep the doc — it's the record of why the design is the way it is.

## Index

| Doc                                | Roadmap IDs                     | Status                            |
|------------------------------------|---------------------------------|------------------------------------|
| [App detail](features/app-detail.md) | FR-10 … FR-18, plus EX-07      | Approved design, not implemented   |
