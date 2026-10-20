# Together 0.30.0 — Android Preview v6

This is a **source project, not a completed messaging replacement**. The new opt-in notification listener can locally capture basic new message notifications from Google Messages and OpenBubbles Alpha after the user grants notification access in Android Settings. It displays those notifications separately from fictional demo messages, and provides a delete control.

## Limitations

- Notifications are not full message histories and cannot guarantee outgoing message coverage, media attachments, or RCS access.
- Notification content may be hidden by app settings, Android privacy controls, or the sender app; notifications may repeat or summarize messages.
- The notification capture file contains sensitive message previews stored in app-private storage; do not use on a shared device. Captured data can be deleted in the preview.
- No Android SDK/Gradle available here to compile or run this project. No APK is included.
- App ID `app.together.preview` does not overwrite installed Together 0.29.37.

## Build

Install Android SDK 35, JDK 17, Gradle 8.9+ and run `gradle :app:assembleDebug` from the project root. The project has not been Android-compiled or device-tested.

## GitHub Actions build

This project now includes `.github/workflows/build-preview.yml`. Upload the project files to a GitHub repository and run **Build Together 0.30 Preview** from Actions, or push to `main`. The workflow provisions Java 17, Android SDK 35 and Gradle 8.9, then uploads the debug APK as a workflow artifact. This workflow has **not yet been executed**; compilation may reveal errors requiring fixes. The APK will be signed with a different debug key and will install as a separate preview app, not as an update to Together 0.29.37.
