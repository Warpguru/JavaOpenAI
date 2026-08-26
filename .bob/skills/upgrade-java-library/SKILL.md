---
name: upgrade-java-library
description: >-
  Use when the user wants to upgrade a Maven dependency to a newer version in a
  Java project. Guides through finding the official changelog, falling back to
  JAR introspection with jar tf and javap when no migration guide exists,
  running a compiler-driven API diff, fixing every broken call-site (including
  @Deprecated warnings surfaced by -Xlint:deprecation), verifying the build and
  tests pass, and recording the full API diff table in the upgrade plan.
metadata:
  disable-model-invocation: true
---

# Upgrade a Java Library Version

Follow these steps in order. Do not skip a step to save time — each step produces
the evidence that the next step requires.

---

## Step 1 — Find the Official Migration Guide First

Before touching any code, search for authoritative documentation:

1. Check the library's GitHub repository for a `CHANGELOG.md`, `MIGRATION.md`, or release notes
   tagged to the target version. Look in the repo root, `docs/`, and GitHub Releases.
2. Check Maven Central for a linked project URL in the POM, then follow it to the documentation
   site.
3. Search for `"<artifactId> <oldVersion> to <newVersion> migration"` in the project's issue
   tracker and wiki.

**If an official migration guide exists:** read it fully before proceeding. Use it as the
authoritative source for Step 5. Skip Step 3 and use the guide instead.

**If no official migration guide exists:** continue to Step 2.

---

## Step 2 — Enable Deprecation Warnings Permanently, Then Bump the Version

### 2a — Enable `-Xlint:deprecation` in `pom.xml` first

Before bumping the version, add `-Xlint:deprecation` to the `maven-compiler-plugin` configuration
if it is not already present. This makes deprecation warnings appear as `[WARNING]` lines with
file and line number on **every** `mvn clean package` run — not buried in a single vague notice
that is easy to miss:

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.15.0</version>
    <configuration>
        <release>21</release>
        <compilerArgs>
            <arg>-Xlint:deprecation</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Without this, Maven suppresses the per-call-site detail and only prints one generic line:
`"Some input files use or override a deprecated API — Recompile with -Xlint:deprecation for details."`
That line is easy to overlook and gives no actionable information. With the flag active, every
deprecated usage is reported with the exact file, line, and symbol name.

### 2b — Bump the version and read the output

1. Update the version property (or direct version) in `pom.xml`. Remove any commented-out
   alternative versions at the same time — leave a clean, single active value.
2. Run `mvn clean package -DskipTests` and capture the full output.
3. Record every `[ERROR]` line — these are **guaranteed** breakages that must change.
4. Record every `[WARNING] ... has been deprecated` line — these are **scheduled removals**
   that must also be fixed or explicitly justified. Both categories are required work.

> Compiler errors and deprecation warnings together form the complete observable API diff for
> your existing call-sites. Step 3 finds the full API diff proactively, including APIs not yet
> called in your code.

---

## Step 3 — Inspect the Downloaded JARs with `javap`

For every class, interface, or record that was used in the old code (not just the ones that
errored), verify its signature in the new JAR using `javap`. This surfaces:

- Constructors that were added, removed, or had parameters changed
- Builder methods that were renamed or had their parameter types changed
- Methods whose return types or argument types changed
- Inner classes that moved to a different outer class or package

### Locating the JAR

The new JAR is downloaded to the local Maven repository during Step 2:
```
C:\Users\<username>\.m2\repository\<groupId-path>\<artifactId>\<version>\<artifactId>-<version>.jar
```

For multi-module SDKs, the main artifact may be a thin aggregator. Use `jar tf` to confirm
whether it actually contains classes, then check sub-module JARs if needed:

```powershell
# List all classes in a JAR (confirms content and finds moved/renamed types)
cmd.exe /c "D:\Development\SetupEnvJava21.cmd && jar tf C:\Users\%USERNAME%\.m2\repository\...\artifact.jar"

# Inspect a specific class or inner class signature
# PowerShell: use backtick to escape the $ in inner class names
$cp = "C:\Users\$env:USERNAME\.m2\repository\...\artifact.jar"
$classes = @("com.example.Outer`$InnerClass", "com.example.SomeBuilder")
foreach ($cls in $classes) {
    Write-Host "=== $cls ==="
    cmd.exe /c "D:\Development\SetupEnvJava21.cmd && javap -classpath $cp $cls" 2>&1
}
```

**What to look for in `javap` output:**

| Pattern | Meaning |
|---------|---------|
| Constructor gone, `$Builder` class appeared | Type migrated from direct constructor to builder pattern |
| Parameter type changed to an SDK-internal mapper type | SDK internalized its JSON dependency — update all transport/builder call-sites |
| BiFunction generic type argument changed | Handler signature changed — all lambdas using that BiFunction must be updated |
| A method is package-private (no `public` modifier) | Cannot be called from user code — find and use the public static factory overload instead |
| A vararg registration method's element type changed | Any collection or spread of those objects must be re-typed throughout |

### Proactive inspection targets

Always inspect these categories, even if they did not appear in compiler errors:

- Every model/schema record used in the project (e.g. `Tool`, `Resource`, `Prompt`, result types)
- Every builder class for those records
- Every transport constructor and builder
- Every handler-specification class (e.g. `AsyncToolSpecification`, `AsyncResourceSpecification`)
- Every factory method on the server/client builder
- The server spec builder's vararg registration methods (`.tools()`, `.resources()`, `.resourceTemplates()`, `.prompts()`)

---

## Step 4 — Build the API Diff Table

Before writing a single line of fix code, produce a written API diff table with one row per
broken or deprecated call-site. Include:

- File name
- Old API (exact call as it existed in source)
- New API (exact replacement, verified by `javap` or sources JAR)
- Change type: one of `constructor removed`, `constructor deprecated`, `method renamed`,
  `type changed`, `class deprecated`, `parameter type changed`

Also record a separate **"Unchanged APIs"** list for everything verified but not changed.
This list is equally important — it is the evidence that prevents over-engineering the migration
and tells future maintainers what is safe to rely on.

**Intentionally-retained deprecations** must also be recorded explicitly — document the reason
(e.g. "entire class deprecated, no non-deprecated replacement exists, kept for feature parity")
so the decision is visible and reviewable.

Append this table to the plan file (or create one if it does not exist) **before** starting any fixes.

---

## Step 5 — Fix Call-Sites from Deepest Dependency Upward

Apply fixes in dependency order:

1. **Shared base classes first** — changes here propagate to all subclasses automatically.
2. **Concrete subclasses next** — fix imports and variable types that changed due to Step 1.
3. **Client/consumer classes last** — these have the widest API surface.

For each file:
- Apply only the changes in the API diff table. Do not refactor, reformat, or clean up unrelated code.
- Run `mvn clean package -DskipTests` after each file or logical group of related files to
  confirm the error count is decreasing, not growing.

---

## Step 6 — Full Build and Test Verification

Once the build is clean with `-DskipTests`, run the full suite:

```
cmd.exe /c "D:\Development\SetupEnvMaven.cmd && D:\Development\SetupEnvJava21.cmd && mvn clean package"
```

Required outcome: **all tests pass, zero failures, zero errors**.

Confirm that every `[WARNING] ... has been deprecated` line in the compiler output is either:
- **Fixed** — replaced with the non-deprecated API, or
- **Intentionally retained** — documented in the API diff table with a justification (e.g. the
  entire class is deprecated but no non-deprecated client replacement exists for the feature).

A finished upgrade has zero unexplained deprecation warnings in the compiler output.

If tests fail at runtime despite compiling cleanly, the most likely cause is a handler signature
change that compiled due to type erasure but fails at runtime dispatch — re-inspect the
BiFunction or Consumer generic type arguments involved using `javap`.

---

## Step 7 — Update Documentation and Plan

1. Update `AGENTS.md` (or equivalent project memory file) to replace every recorded API pattern
   that changed. Add explicit entries for:
   - New constructor or builder patterns (replacing deprecated constructors)
   - Changed parameter types
   - Handler signature changes
   - `@Deprecated` constructors and their exact builder replacements
   - Intentionally-retained deprecated calls and the reason why
   - Unchanged but verified patterns worth recording for future maintainers
2. Mark all plan sub-tasks as `[x] done` and fill in the API diff table in the plan file.
3. Update any hardcoded version strings in source code or documentation files.
4. Update changelog or analysis documents with the new version.

---

## Notes on `javap` Syntax

`javap` is part of the JDK — no additional tools required. Key flags:

| Flag | Effect |
|------|--------|
| *(none)* | Shows public API only (constructors, methods, fields) |
| `-private` | Shows all members including private |
| `-classpath <jar>` | Target a specific JAR |

Inner class names use `$` as separator: `OuterClass$InnerClass`. In PowerShell, escape with
a backtick: `` OuterClass`$InnerClass ``. In cmd.exe, quote the entire class name argument.

When a type lives in a sub-module JAR that itself depends on another JAR, pass both on the
classpath separated by `;` (Windows) or `:` (Linux/macOS).

---

## Why Not Just Rely on Compiler Errors?

The compiler only reports errors at call-sites that exist in the current codebase. It cannot report:

- A handler BiFunction whose second argument type changed — this compiles due to type erasure
  but fails at runtime
- A builder method that was renamed, if that specific method was not called in the existing code
- A transport that migrated from a direct constructor to builder-only, if only one constructor
  path was exercised
- `@Deprecated` usages — the compiler only emits a **warning**, not an error; the build succeeds
  and these are easy to overlook without `-Xlint:deprecation`

`javap` fills these gaps by inspecting the actual bytecode — giving a complete and accurate
picture of the new API regardless of what the existing code happens to call.

When `javap` alone is not sufficient to read inner-class or record signatures (e.g. the tool
resolves to the outer class on Windows due to path escaping), fall back to extracting the
sources JAR from the Maven local repository:

```powershell
# Extract the sources JAR to a temp location inside the project workspace
cmd.exe /c "D:\Development\SetupEnvJava21.cmd && jar xf %USERPROFILE%\.m2\repository\<group-path>\<artifact>-sources.jar io/example/TargetClass.java"
```

Then read the extracted `.java` file to identify the exact constructor signatures, deprecation
annotations, and builder factory methods.
