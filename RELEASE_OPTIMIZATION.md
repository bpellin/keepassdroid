# Release optimization

Both store flavors use R8 code and resource optimization. Debug builds remain
unoptimized. Custom keep rules live in `app/src/main/keepRules/keepass.keep`.
Android's default rules cover JNI names, layout fragments, and XML click handlers.
The app adds narrow rules for JCA provider constructors, Bouncy Castle algorithm
families, and the settings fragments referenced by name in preferences XML.

Built-in database icon IDs 0–68 now reference drawable resources directly;
unknown IDs still use the blank icon. Non-final resource IDs are required by
optimized resource shrinking, so menu dispatch uses comparisons rather than
Java switch cases.

## Build and inspect

Use a full JDK supported by the Gradle wrapper (for example Android Studio's JBR).
The installed SDK must match `app/build.gradle`.

```sh
./gradlew :app:assembleGeneralRelease :app:assemblePlayRelease \
  :app:bundleGeneralRelease :app:bundlePlayRelease :release-tests:assemble
./gradlew :app:lintGeneralRelease :app:lintPlayRelease
python3 scripts/check-release-optimization.py \
  app/build/outputs/bundle/generalRelease/app-general-release.aab \
  app/build/outputs/bundle/playRelease/app-play-release.aab
```

The checker requires nonempty bundled deobfuscation metadata and `r8.json`, prints
DEX bytes and the complement of R8's `no*Percentage` statistics, and fails if any
category is below 25%. The configuration analyzer's scores describe keep rules
before optimization and should not replace the bundle metadata check.

## Runtime validation

The `release-tests` module has no application code imports or implementation
dependency. It tests the optimized APK through Android UI and provider APIs,
and uses reflection only for JNI names which must remain stable in production.
The harness uses the application plugin with an explicit instrumentation manifest
to package its runner dependencies independently of the optimized app. Native
checks invoke the native implementation directly and cannot silently fall back
to Java. A fixture provider supplies writable content URIs for open/save tests.
Argon2d/id expected vectors come from Bouncy Castle 1.86 using password
`page-size-regression`, salt `keepassdroid-salt`, 32 KiB memory, two iterations,
one lane, version 0x13, and 32-byte output.

For local device testing only, the runner signs temporary copies of both APKs
with the standard debug key. It leaves the input release artifacts unchanged
and fails if instrumentation crashes or no tests run. Never publish debug-key
artifacts. Build the release and harness APKs above, then run:

```sh
python3 scripts/run-release-tests.py \
  app/build/outputs/apk/general/release/app-general-release-unsigned.apk \
  release-tests/build/outputs/apk/general/release/release-tests-general-release.apk
python3 scripts/run-release-tests.py \
  app/build/outputs/apk/play/release/app-play-release-unsigned.apk \
  release-tests/build/outputs/apk/play/release/release-tests-play-release.apk
./gradlew :app:connectedGeneralDebugAndroidTest :app:connectedPlayDebugAndroidTest
```

Use the actual APK filename when a signing configuration produces a signed APK
rather than `*-unsigned.apk`. Set `JAVA_HOME` to the full JDK for `apksigner`.
The runner reads `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties`;
`--sdk` and `--serial` override SDK and device selection.

Use dedicated test devices: the runner installs/replaces the app and harness and
uses disposable copies of the bundled database fixtures. Test on API 23 and a
current Android release, including ARM64. Before publishing, manually check
biometric unlock, local/cloud document providers, external file opening, editing
and saving entries, password generation, settings, icons, and restart/reopen.

## Publishing

1. Build with the production signing configuration and an unused version code.
   Repeat the bundle metadata check on the exact signed output.
2. Archive the signed bundle/APK, `app/build/outputs/mapping/<variant>/`, checker
   output, test reports, and source revision together. Preserve the workspace
   diff as well if building an uncommitted revision. Bundles include
   `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` and
   `BUNDLE-METADATA/com.android.tools/r8.json` automatically.
3. Upload the Play bundle to internal testing. Record the affected version and
   deadline from Console, confirm every reported category is at least 25%, and
   check deobfuscated crash reporting.
4. Promote that same validated artifact through a staged rollout before the
   deadline. Monitor crashes, ANRs, and database failures; halt rollout on a
   regression. The server-side Console result remains the final acceptance check.

References: [Enable R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[DEX optimization metrics](https://developer.android.com/google/play/vitals/code-optimization).

## Validation record — 2026-11-01

- General and Play release APKs/bundles and independent test harnesses build.
- Both bundles include nonempty mappings and R8 metadata: obfuscation 94.43%,
  optimization 93.27%, shrinking 94.30%. Each contains 2,168,600 uncompressed
  DEX bytes, versus 14,114,684 (General) and 14,114,672 (Play) in the existing
  unoptimized release APKs: approximately 84.6% less DEX code.
- Bundle checker accepts optimized bundles and rejects synthetic bundles with
  low obfuscation scores or missing mappings.
- Existing debug instrumentation suites: 64 reported cases per flavor, no
  failures, on the Android 16 x86_64 emulator.
- Each optimized release flavor: 12 tests pass on Android 16 x86_64, including
  KDB/KDBX/key files, save/reopen, provider algorithms, JNI AES/final-key,
  Argon2d/id known vectors, all 69 built-in icons, and settings fragment loading.
- Release vital lint passes for both flavors. Full release lint fails on three
  existing `ThreadConstraint` errors in recent-file handling in
  `FileSelectActivity`: background calls to `getDatabaseAt`, `getKeyfileAt`, and
  `deleteFile`. These were not suppressed or added to the baseline.
- API 23, ARM64 runtime testing, biometrics/cloud-provider checks, production
  signing/version allocation, and Play Console verification remain outstanding.
  No Play upload or production rollout was performed.
