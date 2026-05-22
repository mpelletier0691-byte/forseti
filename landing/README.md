# Forseti landing page (GitHub Pages)

Static site for **Forseti** by Asvaettir Labs. Contact: **asvaettirlabs.dev@gmail.com**.

## Files

| File | Role |
|------|------|
| `index.html` | Home page (v0.1.3) |
| `terms-of-use.html` | **Terms of Use** — user agreement, translation & liability |
| `privacy-policy.html` | **Privacy policy** — Play Console URL |
| `terms-of-use.md` / `privacy-policy.md` | Markdown copies for gist sync |
| `styles.css` | Forseti-themed layout |

## GitHub Pages (this repo)

The live site is deployed from the **`docs/`** folder at repo root (not `landing/`):

1. Copy updates: `cp landing/* docs/` (HTML, MD, CSS)
2. Push `main`
3. **Settings → Pages** → branch **main**, folder **`/docs`**

**Live URLs:**

| Page | URL |
|------|-----|
| Home | `https://mpelletier0691-byte.github.io/forseti/` |
| Privacy (Play Console) | `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html` |
| Terms of Use | `https://mpelletier0691-byte.github.io/forseti/terms-of-use.html` |

Paste the **privacy URL** in Play Console → **App content** → **Privacy policy**. Optionally link **Terms** in your store listing or support text.

## Closed testing

Replace the Play button `href` in `index.html` with your **closed-test opt-in link** until the app is public on Production.
