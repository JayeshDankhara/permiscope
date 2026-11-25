# Permiscope

[![Build](https://github.com/JayeshDankhara/permiscope/actions/workflows/build.yml/badge.svg)](https://github.com/JayeshDankhara/permiscope/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

**See which apps can reach your microphone, camera, location and messages, and find out when your camera or microphone switches on.**

Permiscope is a small, open-source privacy dashboard for Android. It has **no internet permission**, so nothing it sees can leave your phone.

## What it shows

**Overview:** how many apps can use each sensitive permission, how many hold powerful special access, and which apps you have not opened for 30 days but that still have sensitive access.

**Apps:** every installed app with an exposure score (Low, Medium or High). Filter by microphone, camera, location, background location, SMS, special access or unused apps. Tap an app to see what each permission means, then jump straight to its system settings to revoke it.

**Activity:** an optional monitor logs every camera and microphone session for 30 days. It alerts you when one starts while the phone is locked or with no app on screen.

### What it checks

| Group | Includes |
|---|---|
| Runtime permissions | Microphone, camera, location (and "all the time"), SMS, call logs, contacts, phone, calendar, body sensors, photos and files, nearby devices |
| Special access | Accessibility services, device admin, notification access, draw over other apps, install unknown apps, usage access |

Special access is where most Android malware hides: banking trojans abuse accessibility and overlays, and stalkerware uses device admin and notification access. Permiscope puts these on the front page.

## Honest limits

- **Android does not tell apps who is using the camera or microphone.** Permiscope knows *that* a sensor is in use, and records the app that was on screen at that moment as the likely user. Android 12 and newer show their own privacy indicators and dashboard. Permiscope adds a 30-day history, lock-screen alerts, older Android versions and the special-access audit.
- Some phones do not report "draw over apps", "install unknown apps" or "usage access" for other apps. Permiscope then shows nothing for those checks rather than guessing.
- The exposure score is a simple weighted sum, meant to help you decide where to look first. It is not a malware verdict.

## Install

Download the latest APK from [Releases](https://github.com/JayeshDankhara/permiscope/releases), or build it yourself:

```bash
git clone https://github.com/JayeshDankhara/permiscope.git
cd permiscope
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
```

Requires Android 8.0 (API 26) or newer.

### Permissions Permiscope uses

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed apps and the permissions they hold |
| `PACKAGE_USAGE_STATS` (you grant it in Settings) | Find unused apps and name the app on screen during camera or microphone use |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the camera and microphone monitor running |
| `POST_NOTIFICATIONS` | Alerts |
| `RECEIVE_BOOT_COMPLETED` | Restart the monitor after a reboot, if you turned it on |

No internet, camera, microphone or location permission is requested.

## Built with

Kotlin, Jetpack Compose and Material 3. There are no third-party libraries, no analytics and no ads.

## Contributing

Bug reports and pull requests are welcome, especially device-specific issues with the app-op checks. Please include your phone model and Android version.

## License

[MIT](LICENSE) © 2026 Jayesh Dankhara
