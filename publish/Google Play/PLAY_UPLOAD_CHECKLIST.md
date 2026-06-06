# Forseti — Play upload checklist (v0.1.3 / versionCode 6)

## Build & staging

- [x] `keystore.properties` on dev PC (never commit)
- [x] Ran `./scripts/stage_play_publish.sh`
- [x] **`Forseti-0.1.3-vc6.aab`** on Desktop
- [x] **`mapping-vc6.txt`** in same Desktop folder
- [x] Git **`4fbd715`** — versionCode 6 on GitHub

## Upload to Play (Production)

- [ ] Upload **`Forseti-0.1.3-vc6.aab`** only (see `PRODUCTION_UPLOAD_STEPS.md`)
- [ ] Upload **`mapping-vc6.txt`** to bundle explorer → version 6
- [ ] Release notes from **`RELEASE_NOTES_PLAY.txt`**
- [ ] **Send for review** / **Start rollout to Production**

## Store & policy

- [ ] Privacy URL: `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`
- [ ] Terms (optional): `https://mpelletier0691-byte.github.io/forseti/terms-of-use.html`
- [ ] GitHub Pages: **main** / **`/docs`**
- [ ] Wix portfolio links updated after go-live

## Smoke test (after live)

- [ ] Play install shows **0.1.3**
- [ ] Pre-language gates → language → disclaimer (translation text)
- [ ] References → FJC section
- [ ] Settings → locked language
