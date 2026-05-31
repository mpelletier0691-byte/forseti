# Forseti landing page (GitHub Pages)

Static site for **Forseti** by Asvaettir Labs. Copy matches the in-app voice (motto, brand copy, tagline). Contact: **asvaettirlabs.dev@gmail.com**.

## Files

| File | Role |
|------|------|
| `index.html` | Home page (v0.1.2 features, Play CTA) |
| `privacy-policy.html` | **Play Console privacy URL** (after Pages deploy) |
| `privacy-policy.md` | Same policy — sync to gist or `asvaettir-privacy-policy` repo |
| `styles.css` | Forseti-themed layout |
| `ASVAETTIR_WIX_ECOSYSTEM.md` | Wix-ready Asvaettir Labs suite copy, portfolio directive, canonical and marketing brand summaries, design system, and publishing checklist |
| `README.md` | This deploy guide |

## Before you publish

1. **Play Store button** — `index.html` uses  
   `https://play.google.com/store/apps/details?id=com.forseti`  
   For **closed testing**, replace `href` with your opt-in link until production is public.

2. **Privacy policy URL (Play Console)** — After GitHub Pages is live, set:
   ```text
   https://mpelletier0691-byte.github.io/<repo-name>/privacy-policy.html
   ```
   Update the gist / `asvaettir-privacy-policy` repo from `privacy-policy.md` so all links stay in sync.

## Deploy to GitHub Pages

```bash
cd ~/Desktop/Projects/Forseti/landing
git init   # only first time
git add index.html privacy-policy.html privacy-policy.md styles.css README.md
git commit -m "Landing v0.1.2 and updated privacy policy"
git branch -M main
git remote add origin https://github.com/mpelletier0691-byte/forseti.git
git push -u origin main
```

Repo → **Settings → Pages** → branch **main**, folder **/ (root)**.

Live URLs (example if repo is `forseti`):

- Home: `https://mpelletier0691-byte.github.io/forseti/`
- Privacy: `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html`

Put the **privacy URL** in Play Console → App content → Privacy policy.

## Sync external gist (optional)

If Play or older links still point at the gist, paste `privacy-policy.md` into:

`https://gist.githubusercontent.com/mpelletier0691-byte/12477f54633425983e1142f292230cba/raw/forseti-privacy.md`

Or commit to `https://github.com/mpelletier0691-byte/asvaettir-privacy-policy`.
