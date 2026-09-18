# AI Workstation — Android Native

AI Workstation is a project-centric Android development workstation using Kotlin + Jetpack Compose. The project keeps persistent prompts, shared checkpoints, work history, provider configuration, a device-local credential vault, API routing, optional Local/Termux execution, and Cloud Build integration points.

## Final build contract

**GitHub Actions is a build machine, not an AI developer.** The workflow performs only:

`CHECKOUT → VERIFY → TEST → GRADLE BUILD → VERIFY APK → UPLOAD ARTIFACT`

There is no Gemini/OpenAI/Claude/Copilot call, no prompt-to-code step, no UI redesign step, and no source rewriting step in `.github/workflows/build-apk.yml`. The workflow also checks that the Git working tree remains clean before and after the build.

The source tree is authoritative. If the source fails verification/tests/build, the workflow stops and reports the real error. It does not invent or silently modify application behavior.

## Cloud toolchain

- Android Gradle Plugin: 9.1.1
- Gradle: 9.3.1
- JDK: 17
- Android SDK platform: 36
- Build Tools: 36.0.0

These versions are pinned to the official AGP 9.1.1 compatibility matrix.

## Android app architecture

- Project Manager
- Create Project / Start Project
- Persistent append-only Project Prompt
- Shared Checkpoint
- Work History / Project Detail
- AI Provider selection
- API Mode foundation
- API Key / Credential Vault using Android Keystore
- Key-pool/routing foundation
- Integrations / Cloud provider configuration surface
- Local Mode with Android → Termux `com.termux.RUN_COMMAND` IPC
- Anti-fake-success rule: Local execution is successful only when Termux returns exit code 0
- Dark/Light/System theme modes
- Configurable accent color
- About / App Information screen

## App information

- App: AI Workstation
- Version: 9.1.0
- Author: STNK Team
- Admin: Project Administrator
- Admin contact: Telegram contact is configured in the About screen; the handle is intentionally hidden from the visible UI and opens the administrator chat when the Telegram logo is tapped.

No fake contact address is embedded.

## Local Mode

Termux is optional. The Android app can send a real command through Termux RUN_COMMAND when Termux is installed and external-app access is enabled. The bridge receives stdout/stderr and the real exit code through a PendingIntent callback.

## Build output

GitHub Actions produces:

`app/build/outputs/apk/debug/app-debug.apk`

The APK is verified as a non-empty file, its SHA-256 is printed, and it is uploaded as the `AI-Workstation-debug-apk` artifact.

## Important

The ZIP contains source/configuration, not the Android SDK or dependency caches. Those are intentionally downloaded by GitHub Actions during the build, so a small source ZIP size is normal.
