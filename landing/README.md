# Forseti landing page (GitHub Pages)

Static one-page site for **Forseti** by Asvaettir Labs. Copy matches the in-app voice (motto, brand copy, tagline). Contact: **asvaettirlabs.dev@gmail.com**.

## Before you publish

1. **Play Store button** — In `index.html`, the primary button points at  
   `https://play.google.com/store/apps/details?id=com.forseti`  
   That works after the app is **public on Production**. Until then, replace `href` with your **closed-test** or **internal-test** opt-in URL from Play Console.

2. **Privacy links** — Footer links use your public gist and GitHub repo. Update if those URLs change.

## Create the GitHub repo

1. On GitHub (logged in as **mpelletier0691-byte**): **New repository**.
2. Name suggestions:
   - **`forseti`** → site URL: `https://mpelletier0691-byte.github.io/forseti/`
   - or **`forseti-landing`** → `https://mpelletier0691-byte.github.io/forseti-landing/`
3. Public, add a README (optional), **Create repository**.

## Upload these files

From your machine (adjust repo name if not `forseti`):

```bash
cd /path/to/Forseti/landing
git init
git add index.html styles.css README.md
git commit -m "Add Forseti landing page"
git branch -M main
git remote add origin https://github.com/mpelletier0691-byte/forseti.git
git push -u origin main
```

Or use the GitHub web UI: **Add file → Upload files** and drag `index.html` and `styles.css` into the repo root.

## Turn on GitHub Pages

1. Repo → **Settings → Pages**.
2. **Build and deployment**: Source = **Deploy from a branch**.
3. Branch = **main**, folder = **/ (root)**.
4. Save. After ~1–2 minutes the site is live at:

   `https://mpelletier0691-byte.github.io/<repo-name>/`

5. Put that URL in Play Console (**Store settings** / **Main store listing** → Website), BetaList, email signature, etc.

## Optional: custom domain

Buy a domain (e.g. at Porkbun, Namecheap), then in **Pages** add the custom hostname and follow GitHub’s DNS instructions. You can keep the `github.io` URL as a redirect target later.

## Optional: form instead of mailto

To collect emails without a backend, sign up at [Formspree](https://formspree.io) (free tier), create a form, and replace the “Email us” button with a small `<form action="https://formspree.io/f/xxxxx" method="POST">` block (see Formspree docs). Keep the mailto as fallback if you prefer zero setup.

## Files

| File        | Role                          |
|------------|-------------------------------|
| `index.html` | Single-page content + CTAs |
| `styles.css` | Forseti-themed layout       |
| `README.md`  | This deploy guide            |
