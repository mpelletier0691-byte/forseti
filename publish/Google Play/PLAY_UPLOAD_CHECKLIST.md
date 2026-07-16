# Forseti — Play upload checklist (v0.1.17 / versionCode 20)

## Build & staging

- [ ] Version in `app/build.gradle.kts`: **versionCode 20**, **versionName 0.1.17**
- [ ] Ran `./scripts/stage_play_publish.sh` after release smoke test
- [ ] **`Forseti-0.1.17-vc20.aab`** staged (NOT vc7 or debug)
- [ ] **`mapping-vc20.txt`** for deobfuscation

## Pre-upload smoke (release build on Pixel / API 35)

- [ ] Edge-to-edge: status bar and nav bar do not overlap titles or FABs (Dashboard, Case Profile, Scanner, overlays)
- [ ] Brokkr Forge: progress notification shows **File N of M**; completion only after all files
- [ ] What's New overlay + **Settings → Help → What's New** guide loads offline
- [ ] **Guides → Ingesting an Existing Case** reflects background ingest
- [ ] **Guides → Staying Organized in Family Court** opens offline (English content)
- [ ] Trial, billing, deadlines notifications still work

## Upload to Play (Production)

- [ ] Upload **`Forseti-0.1.17-vc20.aab`** (see `PRODUCTION_UPLOAD_STEPS.md`)
- [ ] Upload **`mapping-vc20.txt`** → bundle explorer → version **20**
- [ ] Release notes from **`RELEASE_NOTES_PLAY.txt`**
- [ ] **Foreground service** declaration: **Data sync** — Brokkr Forge document sorting
- [ ] **Send for review** / **Start rollout**

## Store & policy (unchanged from 0.1.4)

- [ ] Privacy URL: `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`
- [ ] Terms: `https://mpelletier0691-byte.github.io/forseti/terms-of-use.html`
- [ ] **Does your app use advertising ID?** → **No**
- [ ] Target API **35** shown in Play Console

## After live

- [ ] Play install shows **0.1.17**
- [ ] Pre-language gates → language → disclaimer → tutorial (existing users skip to What's New once)
- [ ] References → FJC section
- [ ] Settings → locked language
