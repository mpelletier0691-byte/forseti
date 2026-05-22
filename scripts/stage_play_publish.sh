#!/usr/bin/env bash
# Build signed release AAB and stage Google Play upload files under ~/Desktop/Publish_Projects/Forseti/
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PUBLISH_ROOT="${PUBLISH_ROOT:-$HOME/Desktop/Publish_Projects}"
PLAY_DIR="$PUBLISH_ROOT/Forseti/Google Play"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

cd "$ROOT"

echo "==> Building release AAB (signed with keystore.properties)…"
./gradlew :app:bundleRelease --no-daemon

AAB_SRC="$ROOT/app/build/outputs/bundle/release/app-release.aab"
MAPPING_SRC="$ROOT/app/build/outputs/mapping/release/mapping.txt"

if [[ ! -f "$AAB_SRC" ]]; then
  echo "ERROR: AAB not found at $AAB_SRC" >&2
  exit 1
fi

# Read version from Gradle (source of truth after build)
VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | head -1 | sed -E 's/.*= ([0-9]+).*/\1/')

STAMP="$(date -Iseconds)"
AAB_NAME="Forseti-${VERSION_NAME}-vc${VERSION_CODE}.aab"
DEST_AAB="$PLAY_DIR/$AAB_NAME"

mkdir -p "$PLAY_DIR/mapping" "$PLAY_DIR/archive"

echo "==> Staging to $PLAY_DIR"
cp -f "$AAB_SRC" "$DEST_AAB"
if [[ -f "$MAPPING_SRC" ]]; then
  cp -f "$MAPPING_SRC" "$PLAY_DIR/mapping/mapping-vc${VERSION_CODE}.txt"
fi

# Keep one archived copy per staging run
cp -f "$DEST_AAB" "$PLAY_DIR/archive/${AAB_NAME%.aab}-${STAMP}.aab"

cat > "$PUBLISH_ROOT/Forseti/VERSION.txt" <<EOF
product=Forseti
applicationId=com.forseti
versionName=$VERSION_NAME
versionCode=$VERSION_CODE
stagedAt=$STAMP
aab=$AAB_NAME
playFolder=Google Play/
EOF

if command -v sha256sum >/dev/null 2>&1; then
  (cd "$PLAY_DIR" && sha256sum "$AAB_NAME" > SHA256SUMS.txt)
fi

BYTES=$(stat -c%s "$DEST_AAB" 2>/dev/null || stat -f%z "$DEST_AAB")
echo ""
echo "Done."
echo "  AAB:     $DEST_AAB"
echo "  Size:    $BYTES bytes"
echo "  Version: $VERSION_NAME (code $VERSION_CODE)"
echo "  Next:    open $PLAY_DIR/START_HERE.md"
