# Google Play — Forseti (start here)

## Where files live

| What | Location |
|------|----------|
| **This checklist (in git)** | `publish/Google Play/` in the Forseti repo |
| **Upload AAB (local)** | `~/Desktop/Publish_Projects/Forseti/Google Play/` after `scripts/stage_play_publish.sh` |

Cloud agents can read this folder from GitHub; they **cannot** access your Desktop or signing keystore.

## 1. Stage the release bundle (on your dev PC)

```bash
cd ~/Desktop/Projects/Forseti
./scripts/stage_play_publish.sh
```

Produces `Forseti-<version>-vc<N>.aab` on your PC (not committed to git).

## 2. Play Console upload (Production)

See **`PRODUCTION_UPLOAD_STEPS.md`** for click-by-click instructions.

Quick path: **Test and release → Production → Upload**  
`~/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.3-vc6.aab`

What's inside the bundle: **`WHAT_IS_IN_VC6.md`**

## 3. Policy URLs (GitHub Pages: main + `/docs`)

- Privacy: `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`
- Terms: `https://mpelletier0691-byte.github.io/forseti/terms-of-use.html`

## 4. Checklist

See `PLAY_UPLOAD_CHECKLIST.md`.
