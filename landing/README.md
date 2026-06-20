# Forseti landing page (GitHub Pages)

Static site for **Forseti** by Asvaettir Labs (Michael David Pelletier).  
Contact: **asvaettirlabs.dev@gmail.com**

## Files

| File | Role |
|------|------|
| `index.html` | Home — product overview + **Official links** hub |
| `privacy-policy.html` | **Privacy Policy** (Play Console policy URL) |
| `terms-of-use.html` | **Terms of Use** (user agreement & liability) |
| `privacy-policy.md` / `terms-of-use.md` | Markdown source copies |
| `styles.css` | Shared Forseti theme (charcoal, rune gold, ash white) |
| `fgs-demo.html` | Brokkr Forge foreground-service demo (Play Console video link) |
| `media/brokkr-forge-demo.mp4` | Screen recording for FGS declaration |

## Deploy

From the Forseti repo root:

```bash
./scripts/sync_landing_site.sh
```

This copies `landing/*` → `docs/` (main repo Pages) and, if present, `../forseti_landing/` (dedicated landing repo).

Then push both repos:

```bash
# Main site (Play-approved policy URLs)
cd ~/Desktop/Projects/Forseti
git add landing/ docs/ scripts/sync_landing_site.sh
git commit -m "Update Forseti landing links and sync site."
git push origin main

# Dedicated landing URL (mirror same files at repo root)
cd ~/Desktop/Projects/forseti_landing
git add index.html privacy-policy.html terms-of-use.html styles.css
git commit -m "Sync Forseti landing site from main repo."
git push origin main
```

## Live URLs (keep in sync)

| Page | Primary (Play Console) | Mirror |
|------|------------------------|--------|
| Home | `https://mpelletier0691-byte.github.io/forseti/` | `https://mpelletier0691-byte.github.io/forseti_landing/` |
| Privacy Policy | `https://mpelletier0691-byte.github.io/forseti/privacy-policy.html` | `https://mpelletier0691-byte.github.io/forseti_landing/privacy-policy.html` |
| Terms of Use | `https://mpelletier0691-byte.github.io/forseti/terms-of-use.html` | `https://mpelletier0691-byte.github.io/forseti_landing/terms-of-use.html` |
| FGS demo video | `https://mpelletier0691-byte.github.io/forseti/fgs-demo.html` | `https://mpelletier0691-byte.github.io/forseti_landing/fgs-demo.html` |
| Google Play | `https://play.google.com/store/apps/details?id=com.forseti` | (same) |

**Play Console → App content → Privacy policy** must use the **`/forseti/privacy-policy.html`** URL unless you change it in Console after verifying the mirror matches.

## GitHub Pages settings

| Repo | Branch | Folder |
|------|--------|--------|
| `forseti` | `main` | `/docs` |
| `forseti_landing` | `main` | `/` (root) |
