#!/bin/sh
set -eu

fail() { echo "VERIFY FAILED: $1" >&2; exit 1; }

[ -f settings.gradle.kts ] || fail "settings.gradle.kts missing"
[ -f build.gradle.kts ] || fail "root build.gradle.kts missing"
[ -f app/build.gradle.kts ] || fail "app/build.gradle.kts missing"
[ -f app/src/main/AndroidManifest.xml ] || fail "AndroidManifest.xml missing"
[ -f gradle/libs.versions.toml ] || fail "version catalog missing"
[ -f gradle/wrapper/gradle-wrapper.properties ] || fail "Gradle version declaration missing"
[ -x ./gradlew ] || fail "gradlew is not executable"
[ -f app/src/main/java/com/example/ui/screens/AboutScreen.kt ] || fail "About screen missing"
[ -f .github/workflows/build-apk.yml ] || fail "GitHub Actions workflow missing"
[ -f app/src/main/java/com/example/bridge/ExecutionBridge.kt ] || fail "real execution bridge missing"
[ -f app/src/main/java/com/example/security/CredentialVault.kt ] || fail "credential vault missing"
[ -f app/src/main/java/com/example/routing/AiRouter.kt ] || fail "AI router missing"

grep -q 'agp = "9.1.1"' gradle/libs.versions.toml || fail "AGP 9.1.1 is not pinned"
grep -q 'distributionUrl=.*gradle-9.3.1-bin.zip' gradle/wrapper/gradle-wrapper.properties || fail "Gradle 9.3.1 is not pinned"
grep -q 'compileSdk = 36' app/build.gradle.kts || fail "compileSdk is not pinned to 36"
grep -Fq "gradle-version: '9.3.1'" .github/workflows/build-apk.yml || fail "CI Gradle version mismatch"
grep -Fq "java-version: '17'" .github/workflows/build-apk.yml || fail "CI JDK version mismatch"
grep -Fq "git diff --exit-code" .github/workflows/build-apk.yml || fail "CI source immutability gate missing"
grep -q 'assembleDebug' .github/workflows/build-apk.yml || fail "APK build task missing"
grep -q 'test -s "\$APK"' .github/workflows/build-apk.yml || fail "APK verification missing"

# Cloud build must remain a build-only pipeline. No AI agent/API is allowed to rewrite source.
if grep -RInE '(gemini|openai|anthropic|claude|copilot|9router|openrouter)' .github/workflows >/tmp/aiw-ci-ai-scan.txt 2>/dev/null; then
  cat /tmp/aiw-ci-ai-scan.txt >&2
  fail "AI/provider execution found in build workflow"
fi

# Do not allow common hard-coded credential patterns into the source tree.
if grep -RInE 'AIza[0-9A-Za-z_-]{20,}|sk-[A-Za-z0-9]{20,}|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}' --exclude-dir=.git --exclude='*.png' --exclude='*.webp' . >/tmp/aiw-secret-scan.txt 2>/dev/null; then
  cat /tmp/aiw-secret-scan.txt >&2
  fail "possible hard-coded credential detected"
fi

echo "VERIFY OK: source structure, pinned toolchain, bridge, vault, router and APK verification gates are present."
