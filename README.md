# RealBuds

An open-source Android companion app for realme Buds Air 8, speaking the
earbuds' own Bluetooth control protocol.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen.svg)](#requirements)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7f52ff.svg)](https://kotlinlang.org)

Full control over ANC, equaliser, touch gestures and device features — plus
several things the official app never exposes.

<p align="center">
  <img src="docs/screenshots/sound.png" alt="Sound tab: ambient sound modes, quick settings, noise cancellation, equaliser and dynamic audio" width="32%">
  &nbsp;&nbsp;
  <img src="docs/screenshots/device.png" alt="Device tab: per-feature toggles reported as supported by the earbuds" width="32%">
</p>

---

## Why this exists

The stock realme Link app is heavyweight and requires an account. RealBuds
talks the same Bluetooth protocol directly: no account, no analytics, and no
network access at all — it declares no `INTERNET` permission, so it cannot
phone home even in principle.

It also does more. While mapping the protocol it became clear the firmware
supports features realme Link never sends:

| Feature                  | realme Link | RealBuds          |
| ------------------------ | ----------- | ----------------- |
| Find my earbuds          | not exposed | yes               |
| Adaptive ANC by motion   | no          | yes               |
| High-volume mode         | buried      | Settings › Audio  |
| Named custom EQ profiles | limited     | rename, six bands |
| Live push updates        | polls       | subscribes        |

Find my earbuds is the clearest example: the earbuds answer the command
readily, but the stock app never asks.

---

## Features

**Noise control** — Off, Transparency, and four cancellation depths
(Adaptive, Max, Moderate, Mild). Adaptive is the firmware's own
ambient-driven mode.

**Adaptive ANC** — switches mode automatically from detected motion
(still / walking / running) using the platform step detector. No Play
Services dependency.

**Equaliser** — four presets plus custom six-band profiles at
62/250/1k/4k/8k/16k Hz, with a draggable curve editor and device-side
renaming.

**Dynamic audio** — three-band low/mid/high adjustment, range read from the
device rather than assumed.

**Controls** — every touch gesture the earbuds report, set per side or
mirrored across both.

**Device features** — 13 toggles reported by the hardware, including game
mode, LHDC, spatial audio, wind-noise reduction and find-my-phone.

**Status** — per-bud battery and case level, wear state (in ear / out / in
case), and lifetime listening hours.

**Interface** — Material 3 with dynamic colour, light and dark themes.

---

## Requirements

- Android 8.0 (API 26) or newer
- realme Buds Air 8, paired over Bluetooth
- Dynamic colour requires Android 12+; older releases use a built-in accent
- Adaptive ANC requires a step-detector sensor (most phones have one)

Other realme models using the same TL protocol may partly work. Feature
availability is read from the device, so unsupported controls are hidden
rather than shown broken.

---

## Permissions

RealBuds asks for as little as it can, and **never requests network access**
— the app has no `INTERNET` permission, so it cannot send your data anywhere
even in principle. There is no account, no telemetry and no analytics.

| Permission | Needed for | Prompted |
|---|---|---|
| `BLUETOOTH_CONNECT` | Opening the control connection to the earbuds | Yes |
| `BLUETOOTH_SCAN` | Listing your paired earbuds | Yes |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | The same, on Android 11 and older | Install time |
| `ACTIVITY_RECOGNITION` | Reading the step counter for Adaptive ANC | Yes, optional |

Three runtime prompts in total. The third is genuinely optional: decline it
and Adaptive ANC reports itself unavailable while every other feature works
normally.

Notably **not** requested:

- **Internet or network state.** Nothing is uploaded, so nothing is asked.
- **Location.** Some companion apps use it for location-based sound profiles;
  Adaptive ANC here uses the step detector instead, which needs no location.
- **Phone or call permissions.** Auto-answer runs in the earbuds' own
  firmware — the app only flips the flag, so it never touches your calls.
- **Notifications.** Nothing is posted to the shade.
- **Storage or contacts.** Settings live in app-private preferences.

## Installation

Download an APK from [Releases](../../releases) and install it, or build from
source. Releases carry one APK per CPU architecture:

| File | For |
|---|---|
| `arm64-v8a` | Almost every phone from 2017 onward. **Start here.** |
| `armeabi-v7a` | Older 32-bit ARM devices. |
| `x86_64` | Emulators and x86 Chromebooks. |
| `universal` | Every architecture in one file, if you are unsure. |

Each release includes `SHA256SUMS.txt`, so a download can be verified:

```bash
sha256sum -c SHA256SUMS.txt --ignore-missing
```

### Building

```bash
git clone https://github.com/<owner>/realbuds-oss.git
cd realbuds-oss
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. To install directly to a
connected device:

```bash
./gradlew installDebug
```

Requires JDK 17 or 21 and the Android SDK (platform 35). Point
`local.properties` at your SDK:

```properties
sdk.dir=/path/to/Android/sdk
```

Note that Android Gradle Plugin 8.7 does not support JDK 25; if your system
JDK is newer, set `org.gradle.java.home` in `gradle.properties`.

### Releases

Pushing a `v*` tag builds the release APKs and publishes them, with the tag
supplying `versionName` and the commit count supplying `versionCode`:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Signing is optional. Set four repository secrets — `KEYSTORE_BASE64` (the
keystore, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD` — and releases are signed with that key so users can upgrade
in place. Without them the build still succeeds using the debug key, and the
release notes say so.

---

## Architecture

```
app/src/main/java/com/realbuds/app/
├── proto/         Bluetooth protocol: framing, commands, parsers
├── adaptive/      Motion detection and ANC rule engine
└── ui/            Compose screens, components and theme
```

One `BudsClient` owns the Bluetooth socket, because the earbuds accept a
single connection, and all state reaches the UI as `StateFlow`.

---

## Protocol

The earbuds are driven over Bluetooth Classic SPP with a length-prefixed
binary protocol. Two behaviours shape the codebase: a success reply does not
mean a write took effect, so writes are followed by read-backs; and the
device's own feature list decides which controls are shown, since writes to
unsupported features are accepted and dropped. The wire format is documented
in comments in `proto/`.

---

## Contributing

Contributions are welcome, particularly support for other realme models.

Useful before opening a pull request:

- Start in `proto/`. The comments there record byte layouts and firmware
  quirks, including which behaviour is confirmed against hardware.
- Test protocol changes on a real device and say what you observed. A write
  that returns success and reads back correctly can still do nothing.
- Do not add proprietary assets. See below.

---

## Licensing and assets

Released under the GNU General Public License v3.0. See [LICENSE](LICENSE).

All artwork is original: the earbud graphics, icons and logo are drawn
vectors. No manufacturer imagery is bundled, deliberately — product renders
are copyrighted and cannot be relicensed under the GPL. Please keep it that way in contributions.

---

## Disclaimer

This project is not affiliated with, endorsed by, or connected to realme or
Guangdong OPPO Mobile Telecommunications. "realme" and "Buds Air" are
trademarks of their respective owners.

RealBuds is an independent implementation written for interoperability. It
contains no manufacturer code and is not derived from any.

Changing device settings carries the usual risk of any third-party tool.
High-volume mode in particular lifts a regional volume limit that exists for
hearing-safety reasons.
