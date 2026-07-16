# Forseti — cloud agent & on-the-go work

Use this when you want **Cursor Cloud Agents** (phone, tablet, browser) to work on Forseti without your Precision Tower.

## Read first

| Doc | Purpose |
|-----|---------|
| **`docs/CURRENT_STATUS.md`** | **Full handoff** — version, Play status, guide gating, next tasks, starter prompt |
| **`docs/GUIDES_PREMIUM_GATING.md`** | Free vs purchase-locked guides (Model B) |
| `docs/FORSETI_PROJECT_BRIEF.md` | Product overview |
| `docs/Forseti_Implementation_Notes.md` | Feature inventory |

---

## Secrets policy (hard rule)

**Do not** paste into chat, commit, or give cloud agents:

- Play Console / Google account passwords or 2FA  
- `keystore.properties`, `*.jks`, signing passwords  
- Bank, billing merchant, or other account credentials  

Cloud agents access **only the GitHub repo** the owner connects in Cursor.  
“Allow any access to accounts” is **not supported** and must be refused.

`keystore.properties` is gitignored. Signing and Play uploads stay on the owner’s PC.

---

## What is in the cloud (GitHub)

**Repo:** https://github.com/mpelletier0691-byte/forseti

| Path | Use |
|------|-----|
| `app/` | Android / Kotlin source (incl. Brokkr Forge, edge-to-edge, guides) |
| `landing/` + `docs/` | Website + agent briefings (GitHub Pages from `/docs`) |
| `publish/Google Play/` | Play checklists & release notes (text only) |
| `scripts/` | Build/install/stage helpers |

**Not in git (stay on PC):**

- `keystore.properties` / upload `.jks`  
- `~/Desktop/Publish_Projects/.../*.aab`  
- Android SDK + emulator (unless cloud VM has them)  
- Any Google / Play login cookies or passwords  

---

## Start a cloud agent on Forseti

1. **Cursor** → **Agents** (Cloud Agents on mobile / web).  
2. GitHub → **`mpelletier0691-byte/forseti`**.  
3. Branch: **`main`**.  
4. Paste the starter prompt from **`docs/CURRENT_STATUS.md`**.

For guides only:

> Read `docs/GUIDES_PREMIUM_GATING.md` and `docs/CURRENT_STATUS.md`. Implement Model B premium guides. Never ask for passwords or keystores.

---

## What works well on a cloud agent

- Kotlin / Compose / strings / guides markdown / `landing` / `docs` / `publish` docs  
- Guide premium gating UI  
- Git branch → PR  

## What still needs the owner’s PC

- `./scripts/stage_play_publish.sh` (keystore + SDK)  
- Emulator / device install  
- Play Console clicks and AAB upload  

Workflow: **edit in cloud → merge PR → pull on PC → build AAB → Play Console.**

---

## After cloud work (on Precision Tower)

```bash
cd ~/Desktop/Projects/Forseti
git pull origin main
./scripts/install_debug.sh
# when ready for Play:
./scripts/stage_play_publish.sh
```

---

## Quick links

- Repo: https://github.com/mpelletier0691-byte/forseti  
- Pages: https://mpelletier0691-byte.github.io/forseti/  
- FGS demo: https://mpelletier0691-byte.github.io/forseti/fgs-demo.html  
- Play Console: https://play.google.com/console (owner login only)  
