# Building XCode Studio

1. Install JDK 17.
2. Install Android SDK Platform 35 and Build Tools 35.0.0.
3. From the project root run `sh ./gradlew :app:assembleDebug`.
4. Find the output at `app/build/outputs/apk/debug/app-debug.apk`.

To build in GitHub Actions, push the project to a GitHub repository. Open Actions, select **Android build**, and run the workflow. The workflow stores the APK artifact for 14 days.

The provided `gradlew` is a small bootstrap shell script, not the standard Gradle wrapper JAR. It downloads the pinned Gradle distribution when first run and therefore needs `curl`/`wget`, `unzip`, and network access.
