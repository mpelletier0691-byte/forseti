# Forseti — publish (in repo for cloud agents)

Play Console checklists and release notes live **in git** so **Cursor Cloud Agents** (and GitHub) can read them on the go.

| In git (here) | On your PC only |
|---------------|-----------------|
| `Google Play/*.md`, `RELEASE_NOTES_PLAY.txt` | Built `.aab` files |
| Upload checklists, policy URLs | `keystore.properties`, upload keystore |

**AAB output (default):** `~/Desktop/Publish_Projects/Forseti/Google Play/`

After `scripts/stage_play_publish.sh`, release notes here are refreshed from the repo copy; the script copies the built AAB to Desktop only.

See [docs/CLOUD_AGENT_WORK.md](../docs/CLOUD_AGENT_WORK.md) for cloud + mobile workflow.
