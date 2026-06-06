# Play Console — upload Forseti 0.1.3 (vc6) step by step

Use this if bundle **6** is not on Production yet, or you need to replace a draft.

## Files on your PC

1. **AAB:** `~/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.4-vc7.aab`
2. **Mapping:** `~/Desktop/Publish_Projects/Forseti/Google Play/mapping/mapping-vc7.txt`
3. **Release notes:** copy from `RELEASE_NOTES_PLAY.txt` in this folder

---

## A. Upload the app bundle

1. Open https://play.google.com/console → select **Forseti**
2. Left menu: **Test and release** → **Production**
3. **Create new release** (or open existing draft with version 6)
4. Under **App bundles**, click **Upload**
5. Choose **`Forseti-0.1.4-vc7.aab`** (never reuse vc6 — Play already registered it)
6. Wait until you see **6 (0.1.3)** with a green check (no errors)

## B. Release notes

Paste text from **`RELEASE_NOTES_PLAY.txt`** into **English (United States)** → Save.

## C. Upload mapping (recommended)

1. **Test and release** → **App bundle explorer**
2. Click version **6**
3. **Downloads** tab or **Deobfuscation files** → **Upload**
4. Select **`mapping-vc6.txt`**

## D. Privacy policy (one time / verify)

1. **Policy and programs** → **App content** → **Privacy policy**
2. URL must be:

   `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`

## E. Countries (if pending)

1. **Publishing overview** — if you see **Add United States**, complete quick checks
2. Click **Send for review** when enabled

## F. Roll out

1. **Production** → **Preview and confirm**
2. Fix any **red errors** (yellow warnings OK for debug symbols)
3. **Start rollout to Production**

Managed publishing **off** = goes live automatically after Google approves.

---

## After approval

- Store: https://play.google.com/store/apps/details?id=com.forseti
- Update Wix Forseti card with that link + GitHub site + privacy URL
