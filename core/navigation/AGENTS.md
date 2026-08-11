# core:navigation Module

## Purpose

Provides the Navigation 3 infrastructure for independent bottom-navigation stacks. The package is
`sk.styk.martin.apkanalyzer.core.navigation`.

## Navigation Model

`NavigationState` owns one stack for top-level history and one sub-stack per top-level key.
Switching tabs must preserve each tab's sub-stack.

`Navigator.navigate()` has three distinct behaviors:

* A different top-level key switches to that stack.
* The current top-level key resets its sub-stack.
* A non-top-level key is pushed onto the current sub-stack.

Back navigation first pops the current sub-stack, then returns to the previous top-level stack. At
the start destination root it invokes the host-provided root-back callback.

Construct state with the complete top-level key set, retain the `Navigator`, and convert state
through `toEntries()` for `NavDisplay`. Do not reproduce stack management in a feature or app host.
