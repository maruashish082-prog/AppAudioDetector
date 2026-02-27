# 🔒 AudioSpy — Privacy Monitor

An Android app that detects which apps are secretly using your **microphone** or playing **audio in the background** — with real-time alerts and a history log.

---

## Features

- 🎙 Detects apps recording audio (microphone) in real-time
- 🔊 Detects apps playing audio in the background
- ⚠️ Instant heads-up alert notification when a new app starts
- 📋 Persistent history log with timestamps (stored locally via Room DB)
- 🔴 Visual highlight for suspicious new entries
- 🗑 Auto-purges logs older than 7 days
- Minimum APK size (~1.5 MB), no unnecessary dependencies

**Requires Android 13+ (API 33)**

---

## 🚀 GitHub Actions — Auto Build APK

Every push to `main` automatically builds a signed release APK and creates a GitHub Release.

### Step 1 — Generate Keystore (one time only)

```bash
keytool -genkey -v \
  -keystore keystore.jks \
  -alias audiospy \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Step 2 — Encode Keystore to Base64

```bash
# macOS / Linux
base64 -i keystore.jks | tr -d '\n'

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks"))
```

### Step 3 — Add GitHub Secrets

Go to: **Repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64 string from Step 2 |
| `STORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `audiospy` |
| `KEY_PASSWORD` | Your key password |

### Step 4 — Push to GitHub

```bash
git init
git remote add origin https://github.com/YOUR_USERNAME/AudioSpy.git
git add .
git commit -m "feat: initial commit"
git branch -M main
git push -u origin main
```

### Step 5 — Download APK

Go to **Actions** tab → click latest run → download from **Artifacts**

Or go to **Releases** tab for the auto-created release.

---

## Workflow Triggers

| Trigger | Action |
|---|---|
| Push to `main` | Build APK + create GitHub Release |
| Push to `develop` | Build APK + upload artifact only |
| Pull Request to `main` | Build to validate (no release) |
| Manual (workflow_dispatch) | Trigger build from GitHub UI |

---

## Project Structure

```
app/src/main/java/com/audiospy/
├── MainActivity.kt           — UI, live status, history list
├── AudioMonitorService.kt    — Foreground service, polling + callbacks
├── AppAudioDetector.kt       — Detects mic/audio via AudioManager APIs
├── AlertManager.kt           — Heads-up alert notifications
├── model/AudioApp.kt         — Data model
├── db/
│   ├── AudioLogEntity.kt     — Room entity
│   ├── AudioLogDao.kt        — Room DAO with Flow queries
│   └── AudioLogDatabase.kt   — Room database singleton
└── ui/
    └── HistoryAdapter.kt     — RecyclerView adapter for history log
```

---

## How It Works

**Microphone detection** uses `AudioManager.registerAudioRecordingCallback()` — fires instantly when any app starts or stops recording. No polling needed.

**Playback detection** uses `AudioManager.getActivePlaybackConfigurations()` polled every 2 seconds — no callback API exists for playback.

Both methods resolve app UID → package name → app label using `PackageManager`. No special permissions required beyond `FOREGROUND_SERVICE`.

---

## Privacy Note

This app only reads **audio routing metadata** — it never records, stores, or transmits any actual audio content. It uses the same system-level data that Android 12's green mic indicator uses, just made visible in a detailed UI.
