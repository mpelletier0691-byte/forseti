# Forseti — cloud agent & on-the-go work

Use this when you want **Cursor Cloud Agents** (phone, tablet, browser) to work on Forseti without your Precision Tower.

## What is already in the cloud

**GitHub repo:** https://github.com/mpelletier0691-byte/forseti

| Path | Use |
|------|-----|
| `app/` | Android / Kotlin source |
| `landing/` + `docs/` | Website (GitHub Pages from `/docs`) |
| `publish/Google Play/` | Play checklists & release notes |
| `scripts/` | `stage_play_publish.sh`, `install_debug.sh` |
| `docs/DEBUG_TESTING.md` | Debug APK clean slate |
| `docs/GUIDES_PREMIUM_GATING.md` | **Free vs purchase-locked guides** (trial Model B, new-guide defaults, implementation checklist) |

**Not in git (stay on your PC):**

- `keystore.properties` / upload `.jks` — signing secrets
- `~/Desktop/Publish_Projects/.../*.aab` — release bundles (~35 MB)
- Android SDK + emulator (unless cloud VM has them)

---

## Start a cloud agent on Forseti

1. **Cursor** → **Agents** (or Cloud Agents on mobile / web).
2. Choose **GitHub** → repository **`mpelletier0691-byte/forseti`**.
3. Branch: **`main`** (or a feature branch after this doc is pushed).
4. Give a task, e.g. “Update Spanish strings for References tab” or “Fix issue in ForsetiShell first-run flow.”

For **guide lock/unlock or new premium guides**, point the agent at:

> Read `docs/GUIDES_PREMIUM_GATING.md` and follow it. Free = Forseti how-to guides only; all procedure guides and new guides are purchase-only (Model B).

The agent clones the repo on a Cursor VM, edits, and can open a **PR** for you to merge.

**Important:** This briefing is only available to cloud agents **after** it is committed and pushed to GitHub. On your PC:

```bash
cd ~/Desktop/Projects/Forseti
git add docs/GUIDES_PREMIUM_GATING.md docs/CLOUD_AGENT_WORK.md
git commit -m "docs: free vs purchase-gated guides briefing for cloud agents"
git push origin main
```

---

## What works well on a cloud agent

- Kotlin / Compose / strings / `landing` / `docs` / `publish` docs  
- Reading Play checklists and drafting release notes  
- Git commits on a branch → PR  

## What still needs your dev PC (or CI later)

- **`./scripts/stage_play_publish.sh`** — needs upload keystore + Android SDK  
- **`./scripts/install_debug.sh`** — needs emulator or USB device  
- **Play Console clicks** — browser only  
- **Uploading `.aab`** — file is on Desktop after staging  

Workflow: **edit in cloud → merge PR → pull on PC → build AAB → upload to Play.**

---

## Keep Publish_Projects in sync

Your Desktop folder is for **binaries**:

```text
~/Desktop/Publish_Projects/Forseti/Google Play/*.aab
```

The repo folder `publish/Google Play/` is for **text** (checklists, release notes). After you change release notes in git, pull on PC before the next Play upload.

Optional: set the same root when staging:

```bash
export PUBLISH_ROOT="$HOME/Desktop/Publish_Projects"
./scripts/stage_play_publish.sh
```

---

## Secrets for cloud CI (optional, later)

Never commit `keystore.properties`. For a future GitHub Action you could use repo **Secrets**:

- `FORSETI_KEYSTORE_FILE` (base64 keystore)  
- `FORSETI_KEYSTORE_PASSWORD`  
- `FORSETI_KEY_ALIAS`  
- `FORSETI_KEY_PASSWORD`  

`app/build.gradle.kts` already reads those env vars. Cloud agent sessions should **not** be given production keystore passwords in chat.

---

## Pull latest on Precision Tower after cloud work

```bash
cd ~/Desktop/Projects/Forseti
git pull origin main
./scripts/install_debug.sh    # test on emulator
# when ready for Play:
./scripts/stage_play_publish.sh
```

---

## Two-machine mental model

```text
Cloud agent  →  edits code/docs in GitHub PR
       ↓
Your PC        →  pull, emulator/debug, sign AAB, Play Console
       ↓
Testers        →  Play closed testing (same opt-in link)
```

---

## Quick links

- Repo: https://github.com/mpelletier0691-byte/forseti  
- Pages site: https://mpelletier0691-byte.github.io/forseti/  
- Play Console: https://play.google.com/console  
