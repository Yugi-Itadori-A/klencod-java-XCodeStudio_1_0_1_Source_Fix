# XCode Studio 1.0.0 — Android source release

XCode Studio is a native Java Android application for maintaining small-to-medium source projects directly on an Android device. The UI is implemented with Android Views and the app has a real launcher `MainActivity`.

## Build

Requirements: JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The Gradle bootstrap script downloads Gradle 8.11.1 on first use.

```bash
sh ./gradlew :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. In GitHub, push this project to a repository and run **Actions → Android build → Run workflow**. The workflow uploads the debug APK as an artifact.

## Implemented in this source

- Native launcher interface with Projects, Editor, GitHub, AI, Security, Extensions and Settings screens.
- Local project creation from Empty, Android Java, Web, Python, CMake/C++ and Flutter starter templates.
- Local project listing, rename, delete, file creation, file editing and save.
- ZIP import and export through Android's Storage Access Framework. ZIP extraction blocks path traversal and limits archive/expanded sizes and entry count.
- Basic syntax coloring in the text editor and basic project-structure diagnostics.
- GitHub token storage with Android Keystore-backed AES-GCM encryption. GitHub account validation, repository listing/creation, repository ZIP import, project upload, workflow dispatch, run history, artifact listing and ZIP download via the Android document picker.
- AI provider clients for OpenAI-compatible chat APIs, Gemini and Claude. API keys are encrypted locally; source text is attached only when the explicit consent checkbox is selected.
- Optional VirusTotal file upload and analysis lookup after a separate user confirmation, including SHA-256 reporting.
- Extension metadata registry with disabled-by-default entries. Imported extension code is not executed.
- Theme choice and a 16-language selector; RTL direction is applied for Arabic, Urdu and Persian. Core navigation labels are translated, while some feature screens and dialogs remain Arabic.

## Important technical boundaries

This is a functional Android source release, not a full desktop-class IDE. The editor currently provides plain-text editing plus lightweight token coloring; it does not include Language Server Protocol, compiler diagnostics, debugger, terminal, code completion, refactoring or an integrated emulator.

The app does not compile every listed language locally. Android APK builds are performed by the included GitHub Actions workflow after the project is pushed to GitHub. The workflow is configured to build this Android app; user projects need their own compatible build configuration/workflow.

GitHub and provider features require the user's own tokens/API keys, internet access, correct permissions and applicable provider quotas. VirusTotal uploads transmit the selected APK to VirusTotal; users must review its terms and privacy implications. A clean scan result is not proof that a file is completely safe.

The extension screen is a metadata registry only; there is no plugin runtime or marketplace. GitHub repository import/upload uses REST archive/Git object APIs and is subject to API permissions and rate limits.

## Security notes

- API tokens are stored encrypted with AES-GCM using a key held by Android Keystore.
- API keys are not written to project files.
- AI source code is not attached unless the user checks the consent box.
- ZIP extraction rejects absolute paths and traversal outside the destination.
- Imported extension code is never run by the app.
- Network operations run on a background executor.

## Application identifiers

- Package: `com.xio.xcodestudio`
- Minimum Android: 8.0 (API 26)
- Target/compile SDK: 35
- Java source compatibility: 17
