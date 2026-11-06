---
name: write-unit-tests
description: Use when writing or changing unit tests, fakes, or test fixtures in a core module, or when a bug fix needs a regression test. Defines the one test style for this repo. Triggered by phrases like "add a test", "write unit tests", "test this class", "add a fake", "add a test fixture", "regression test".
---

# Write Unit Tests

Every test in this repo looks the same. Copy the shape of
[`CertificateExtractorImplTest`](../../../core/apps/src/test/kotlin/sk/styk/martin/apkanalyzer/core/apps/signing/CertificateExtractorImplTest.kt).

```kotlin
class CertificateExtractorImplTest {

    private val extractor: CertificateExtractor = CertificateExtractorImpl(DigestManager())

    @Test
    fun `Given an organization with a comma When getting signing Then organization keeps the comma`() {
        val packageInfo = aPackageInfo(aSignature(subject = """O=Google\, Inc,C=US"""))

        val signing = extractor.getAppSigning(packageInfo)

        signing.currentCertificates.single().subject shouldBe CertificatePrincipal(organization = "Google, Inc", country = "US")
    }
}
```

## What Deserves a Test

Write a test only where it brings real value. A few tests that catch real bugs beat many that only
add coverage. There is no coverage target, and writing no tests for a class is a valid outcome.

Before writing each test, name the realistic bug it would catch: what mistake in the code makes this
test fail? If you can't name one, don't write the test. A small mistake still counts, such as swapped
fields or dropped items. If you can name the bug and the test is cheap, write it.

Worth testing:

* Parsing and formatting, especially of untrusted input such as certificates and manifests.
* Branching logic and decisions, such as trust levels, filters, sorting, and classification.
* Rules from a module's `AGENTS.md` that look wrong without explanation and are easy to break.
* Stored data compatibility, such as encoding, decoding, and schema changes.
* Anything that has already broken in production.

Not worth testing:

* Code that only passes a call along. For a mapping, one test with distinct values in every field is
  enough to catch swapped fields, so don't add a test per field.
* Composables, Hilt wiring, simple data classes, and getters.
* Library, framework, or language behavior, and fakes or fixtures themselves.
* The same behavior again with different data, unless the data takes a different path through the code.
* Anything that restates the implementation, so it can only fail when the code is changed on purpose.

A passing test proves nothing until it has failed. For logic that matters, temporarily plant the bug
the test is meant to catch, confirm the test fails, then restore the code.

When you finish, say which behaviors you tested and which you skipped and why. If asked to test a class
that has nothing worth testing, say so instead of writing filler.

## Scope

* Unit tests cover `core` modules, on the JVM, under `src/test/kotlin`.
* **Always test through the class's public interface.** Never change production code to make it
  testable: no new `internal` functions, no widened visibility, no extraction into a new file.
* When the public interface takes Android framework types, keep each test short by hiding them behind
  `a<Thing>` helpers, as `CertificateExtractorImplTest` does for `PackageInfo` and `Signature`.
* Helpers used by one test class stay in that test's file as `private` functions, below the class.
* Every crash fix gets a regression test, named after the behavior rather than the issue number.

JVM tests don't prove Android behavior. The JVM has classes Android lacks (`javax.naming.ldap`
crashed in production this way), and platform implementations can format values differently.

Android static fields are null on the JVM, so anything that reads one during construction can't run
here. `AppFunctionException` subclasses read `Bundle.EMPTY`, so `core:app-functions` error paths have
no unit tests. Leave a path like that untested rather than working around it.

[`GetAppCertificatesUseCaseTest`](../../../core/app-functions/src/test/kotlin/sk/styk/martin/apkanalyzer/core/appfunctions/usecase/GetAppCertificatesUseCaseTest.kt)
is the reference for a test that uses a fake and fixtures from another module's `testFixtures`.

## Setup

Apply the plugin in the module's `build.gradle.kts`. Never declare test libraries in a module:

```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.tests.unit)
}
```

`apkanalyzer.tests.unit` enables `testFixtures` and supplies JUnit 4, Kotest assertions, MockK, and
`kotlinx-coroutines-test`. A test library the plugin doesn't supply yet goes into the plugin. Turbine
is the approved addition, added when the first test needs it. A fixtures source set can't compile
without coroutines because of the repo-wide opt-in flags, so the plugin adds them.

A module uses another module's fakes and fixtures with
`testImplementation(testFixtures(projects.core.common))`. A module sees its own automatically.

Run one module's tests with `./gradlew :core:apps:testDebugUnitTest`.

## Test Shape

* Name each test `` `Given x When y Then z` ``, describing behavior rather than method names.
* Avoid `.`, `:`, `/`, `\`, `;`, `<`, `>`, `[`, and `]` in names, and keep them short enough to read.
* Write three blocks for given, when, and then, separated by one blank line. No comments, so no
  `// Given` markers.
* Test one behavior. Several assertions are fine when they describe the same outcome.
* No `if`, or shared helpers that hide the action under test. A loop is only fine for building test
  data.
* Write expected values as literals. Never compute them with production code or read them from a
  fixture.
* Assert with Kotest matchers (`shouldBe`). Compare whole objects when that states the outcome,
  because Kotest then reports a per-field diff.
* One subject per `<Subject>Test`, in the subject's package. Build the subject directly, without
  Hilt.
* Declare the subject and its dependencies as properties at the top of the test class, not inside
  test methods or a helper. JUnit creates a new instance per test, so each test starts clean.

```kotlin
class GetAppCertificatesUseCaseTest {

    @get:Rule
    val dispatcherProvider = TestDispatcherProvider()

    private val appDetailRepository = AppDetailRepositoryFake(WhatsApp)
    private val useCase = GetAppCertificatesUseCase(appDetailRepository, dispatcherProvider)

    @Test
    fun `Given only past certificates When getting certificates Then result is empty`() = runTest {
        appDetailRepository += WhatsApp.copy(signing = AppSigning(pastCertificates = listOf(WhatsAppCertificate)))

        val certificates = useCase(WhatsApp.packageName)

        certificates.shouldBeEmpty()
    }
}
```

## Test Doubles

Prefer them in this order:

1. **The real class**, when it's pure and has no Android or I/O dependency.
2. **A fake**, for any interface we own.
3. **MockK**, only for types we can't fake, such as Android framework or final third-party classes.
   Never mock our own interfaces or data classes, and never use `relaxed = true`.

Fakes:

* Live in the `testFixtures` source set of the module that owns the interface, in the interface's
  package, named `<Interface>Fake`.
* Implement behavior as simply as possible, backed by in-memory state.
* Can always be initialized through the constructor, preferably with a `vararg` of domain objects.
  The constructor holds the setup shared by the whole test class.
* Let individual tests change that setup with an operator. Use `operator fun plusAssign` when the
  value already identifies itself (`fake += WhatsApp.copy(...)`), replacing any existing entry with the
  same identity. Use `operator fun set` only when the key isn't part of the value
  (`fake["key"] = value`).
* Take domain objects directly and work out any keys themselves. No `Result` wrapping or key pairs in
  the constructor.
* Keep the interface's contract: they never throw, and they report failure as `Result.failure`,
  `null`, or an empty collection.

Verify state, not invocations. Assert on what the subject returned or what changed in a fake. Use
MockK's `verify` only when the call is the only visible result.

## Fixtures

Tests never build large classes by hand. Choose by what the test is about:

* **A preset** when any realistic object will do: `Detail.ApkAnalyzer`, `Detail.WhatsApp`, and
  `Detail.Gmail` for `AppDetail`, and their certificates in `Certificates.kt`.
* **A factory** when the test is about differences between a few fields, such as filtering and
  sorting, so the input and the expected result are both on screen. See `anApp` and `anAppSummary` in
  [`FindAppsUseCaseTest`](../../../core/app-functions/src/test/kotlin/sk/styk/martin/apkanalyzer/core/appfunctions/usecase/FindAppsUseCaseTest.kt).

Factories:

* Are named `a<Class>` or `an<Class>`, and take only the fields tests vary, with fixed defaults for
  the rest. A factory for the input and one for the expected result share the same defaults.
* Stay as `private` functions in the test file until a second test class needs them, then move to
  `testFixtures`.
* Use names that explain the test, like `Large`, `Medium`, and `Small`.
* Start the fake empty, so a test only has the items it adds.

Presets:

* Group presets of one type in an `object`, and import the member so tests read `WhatsApp`, as in
  `import ...model.Detail.WhatsApp`.
* Presets are constructed in full with every field set, and composed from other presets rather than
  built on factory defaults.
* Use real app names and package names, but made-up certificates and hashes, so nobody takes fixture
  data for real signing data.
* Use a preset as it is when the test doesn't depend on a field's value, or only depends on its name
  or package name, because those identify the app.
* When an assertion depends on any other value, set it in the test with `.copy(...)` and assert a
  literal:

```kotlin
val certificate = WhatsAppCertificate.copy(issuer = CertificatePrincipal(organization = "Acme"))
appDetailRepository += WhatsApp.copy(signing = AppSigning(currentCertificates = listOf(certificate)))

val certificates = useCase(WhatsApp.packageName)

certificates.single().issuerOrganization shouldBe "Acme"
```

* Refer to a preset's identity through the preset, as in `useCase(WhatsApp.packageName)`, because the
  value only has to match the app.
* Data used by a single test class stays in that test's file.

## Coroutines

* A subject that takes `DispatcherProvider` gets a `TestDispatcherProvider` from `core:common`'s
  `testFixtures`, declared with `@get:Rule` as the first class property. Never pass the real
  implementation.
* The rule replaces `Dispatchers.Main`, so a plain `runTest { }` shares its scheduler. That keeps
  everything on the test thread and skips delays. Forgetting `@get:Rule` fails with "Detected use of
  different schedulers".
* No real delays.
