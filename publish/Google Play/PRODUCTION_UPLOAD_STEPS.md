# Play Console — upload Forseti 0.1.17 (vc20) step by step

Use this for **Production** (or **Internal testing** first — same AAB).

## Files on your PC

1. **AAB:** `~/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.17-vc20.aab`
2. **Mapping:** `~/Desktop/Publish_Projects/Forseti/Google Play/mapping/mapping-vc20.txt`
3. **Release notes:** copy from `RELEASE_NOTES_PLAY.txt` in this folder

Build and stage:

```bash
cd ~/Desktop/Projects/Forseti
./scripts/stage_play_publish.sh
```

---

## A. Upload the app bundle

1. Open https://play.google.com/console → select **Forseti**
2. Left menu: **Test and release** → **Production** (or **Internal testing** for smoke first)
3. **Create new release**
4. Under **App bundles**, click **Upload**
5. Choose **`Forseti-0.1.17-vc20.aab`** (do not reuse vc7 or older bundles)
6. Wait until you see **20 (0.1.17)** with a green check (no errors)

## B. Release notes

Paste text from **`RELEASE_NOTES_PLAY.txt`** into **English (United States)** → Save.

## C. Upload mapping (recommended)

1. **Test and release** → **App bundle explorer**
2. Click version **20** (0.1.17)
3. **Downloads** tab or **Deobfuscation files** → **Upload**
4. Select **`mapping-vc20.txt`**

## D. Privacy policy (one time / verify)

1. **Policy and programs** → **App content** → **Privacy policy**
2. URL must be:

   `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`

## E. Countries (if pending)

1. **Test and release** → **Production** → **Countries / regions**
2. Ensure target countries are selected

## F. Declarations (verify each release)

- **Target API level:** 35
- **Advertising ID:** No
- **Foreground service:** Data sync — Brokkr Forge document sorting (background case ingest)

## G. Rollout

1. **Preview and confirm**
2. **Start rollout to Production** (or save Internal release and test first)

---

## Verify signing (optional, after staging)

```bash
AAB="$HOME/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.17-vc20.aab"
unzip -p "$AAB" META-INF/*.RSA | keytool -printcert | grep SHA256
# Expect upload cert SHA256 matching docs/RELEASE_SIGNING.md
```

See **`WHAT_IS_IN_VC20.md`** for full changelog since production 0.1.4.
