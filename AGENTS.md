# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Purpose

Java 21 tutorial for the OpenAI API. Produces a single uber-jar (`target/JavaOpenAI-1.0.0.jar`) launched via `java -jar JavaOpenAI-1.0.0.jar <command>`. Works against OpenAI cloud and any OpenAI-compatible local LLM server via configurable base URL.

---

## Critical Environment Setup (Windows — MUST run before any Maven or Java command)

```cmd
D:\Development\SetupEnvJava21.cmd
D:\Development\SetupEnvMaven.cmd
```

Without these, `mvn` and `java` point to wrong versions.

---

## Build & Test Commands

```cmd
mvn package                                            # build uber-jar
mvn test                                               # run all integration tests
mvn test -Dtest=JavaOpenAIIntegrationTest#<methodName>    # run a single test
mvn package -DskipTests                                # build without running tests
java -jar target/JavaOpenAI-1.0.0.jar <command>       # run an example
```

Available commands: `config`, `chat`, `stream`, `embed`, `vision`, `reason`, `tts`, `stt`, `imagegen`, `moderate`

---

## JUnit Version

`junit-jupiter 6.1.3` in `pom.xml` is **correct** — JUnit released a real 6.x major in 2024/2025. `maven-surefire-plugin 3.5.3` is required for JUnit 6 platform detection. Do not downgrade either.

---

## SDK: `com.openai:openai-java 4.52.0`

Official first-party OpenAI Java SDK (Kotlin source). All request/response objects use immutable builders. When API class names are uncertain, look them up from the sources jar — see technique below.

### Key SDK gotchas (discovered from source inspection)

| Issue | Correct API |
|---|---|
| `ChatCompletionUserMessageParam` has no `addContentPart()` | Use `.contentOfArrayOfContentParts(List<ChatCompletionContentPart>)` |
| `ChatCompletionContentPart` factory methods | `ChatCompletionContentPart.ofImageUrl(...)` / `.ofText(...)` |
| Vision image field | Pass base64 data URL (`data:image/png;base64,...`) — local servers cannot fetch raw HTTP URLs |
| `SpeechCreateParams.Voice.NOVA` does not exist | `Voice` is a sealed union; use `.voice("nova")` (string overload) |
| `TranscriptionCreateParams.model()` | Takes `AudioModel` (`com.openai.models.audio.AudioModel`), NOT `TranscriptionModel` |
| `client.audio().transcriptions().create()` | Returns `TranscriptionCreateResponse` (sealed union); call `.asTranscription().text()` |
| `ImageGenerateParams.Model.DALL_E_2` does not exist | Use `ImageModel.DALL_E_2` from `com.openai.models.images.ImageModel` |
| `ImagesResponse.data()` | Returns `Optional<List<Image>>`; use `.ifPresent(images -> ...)` |
| `Moderation.categories()` | Returns `Moderation.Categories` directly (not `Optional`) |
| `Moderation.Categories.hate()` etc. | Returns primitive `boolean`, not `Optional<Boolean>` |
| `ImageGenerateParams.Size._512X512` does not exist | Use `.size("512x512")` (string overload) |

### Sources jar lookup technique (PowerShell)

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = "$env:USERPROFILE\.m2\repository\com\openai\openai-java-core\4.52.0\openai-java-core-4.52.0-sources.jar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
# List matching entries:
$zip.Entries | Where-Object { $_.FullName -match 'ClassName' } | Select-Object -ExpandProperty FullName
# Read a specific entry:
$e = $zip.Entries | Where-Object { $_.FullName -eq "main/com/openai/models/.../ClassName.kt" } | Select-Object -First 1
$ms = New-Object System.IO.MemoryStream; $s = $e.Open(); $s.CopyTo($ms); $s.Dispose()
[System.Text.Encoding]::UTF8.GetString($ms.ToArray())
$zip.Dispose()
```

---

## Logging Architecture

Single logger per class in [`src/main/resources/log4j2.xml`](src/main/resources/log4j2.xml):

```
LogManager.getLogger(MyClass.class)   →  edu.java hierarchy
```

| Appender | Target | Format | Receives |
|---|---|---|---|
| `ConsoleAppender` | stdout | `%msg%n` (plain) | INFO only |
| `FileAppender` | `JavaOpenAI.log` | full timestamp/level/location | ALL levels |

Use `logger.info(...)` for all human-facing output — it appears plain on the console and with full detail in the log file. Use `logger.debug(...)` / `logger.error(...)` for diagnostics that should go to the file only.

**Shade plugin requirement:** Log4j2 needs `Log4j2PluginCacheFileTransformer` from `log4j-transform-maven-shade-plugin-extensions:0.2.0` as a plugin `<dependency>` inside the shade plugin — without it, logging breaks silently in the uber-jar.

---

## Configuration

Resolution order: env var → `config.properties` (classpath, gitignored) → hard-coded default.

All 9 config keys with defaults:

| Key | Default |
|---|---|
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` |
| `OPENAI_API_KEY` | `""` (empty — providers return an auth error, not an NPE) |
| `OPENAI_MODEL` | `gpt-4o-mini` |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` |
| `OPENAI_REASONING_MODEL` | `o4-mini` |
| `OPENAI_TTS_MODEL` | `tts-1` |
| `OPENAI_STT_MODEL` | `whisper-1` |
| `OPENAI_IMAGE_MODEL` | `dall-e-2` |
| `OPENAI_MODERATION_MODEL` | `omni-moderation-latest` |

Copy `config.properties.example` → `config.properties` to configure.

---

## Test Configuration

Tests load from `src/test/resources/test.properties` (gitignored) via `TestConfig.java`. Copy `test.properties.example` to configure. Keys: `test.base.url`, `test.api.key`, `test.model`, `test.vision.model`, `test.reasoning.model`, `test.audio.enabled`, `test.image.generation.enabled`, `test.moderation.enabled`.

Reasoning test is skipped automatically when `test.reasoning.model` is blank (`@EnabledIf("reasoningModelConfigured")`).

---

## Vision: Always Use Base64

`VisionExample.toBase64DataUrl(url)` downloads the image and converts it to `data:image/<mime>;base64,...`. Use this in tests too — local servers cannot fetch external URLs.

---

## pom.xml Resources Filter

`src/main/resources` filtering `<includes>` explicitly lists `log4j2.xml` and `config.properties`. Any new classpath resource must be added there or it will not be included in the jar.

---

## Version Constant

`JavaOpenAI.JAVAOPENAI_VERSION` is a string constant used in the usage banner. Keep it in sync with `pom.xml` version manually — there is no auto-substitution at runtime.
