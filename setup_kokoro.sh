#!/usr/bin/env bash
# Run this ONCE on your Mac before building.
# Downloads sherpa-onnx AAR and Kokoro TTS model, bundles them for the Android build.
set -e

SHERPA_VERSION="1.13.8"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIBS_DIR="$SCRIPT_DIR/app/libs"
ASSETS_DIR="$SCRIPT_DIR/app/src/main/assets"
TMP_DIR="$(mktemp -d)"

echo "=== Sanjeevani Kokoro TTS Setup ==="
echo ""

# ── 1. sherpa-onnx Android AAR ────────────────────────────────────────────────
AAR="$LIBS_DIR/sherpa-onnx-android.aar"
if [ -f "$AAR" ]; then
    echo "✓ sherpa-onnx AAR already present"
else
    echo "→ Downloading sherpa-onnx v${SHERPA_VERSION} AAR (~25 MB)…"
    curl -L --progress-bar \
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/sherpa-onnx-${SHERPA_VERSION}.aar" \
        -o "$AAR"
    echo "✓ AAR downloaded"
fi

# ── 2. Kokoro model ───────────────────────────────────────────────────────────
ZIP="$ASSETS_DIR/kokoro.zip"
if [ -f "$ZIP" ]; then
    echo "✓ kokoro.zip already present in assets"
else
    echo ""
    echo "→ Downloading Kokoro model archive (~340 MB)…"
    TARBALL="$TMP_DIR/kokoro.tar.bz2"
    curl -L --progress-bar \
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2" \
        -o "$TARBALL"

    echo "→ Extracting…"
    tar -xjf "$TARBALL" -C "$TMP_DIR"

    MODEL_DIR="$TMP_DIR/kokoro-en-v0_19"
    if [ ! -d "$MODEL_DIR" ]; then
        echo "ERROR: Expected $MODEL_DIR after extraction"
        exit 1
    fi

    echo "→ Repacking as kokoro.zip…"
    # Zip with store (no compression) so Android ZipInputStream reads it
    # without double-decompression overhead.
    cd "$MODEL_DIR"
    zip -r -0 "$ZIP" .
    cd "$SCRIPT_DIR"
    echo "✓ kokoro.zip created ($(du -sh "$ZIP" | cut -f1))"
fi

rm -rf "$TMP_DIR"

echo ""
echo "=== Setup complete ==="
echo "Now run:  ./gradlew assembleDebug"
echo ""
echo "First app launch will extract the model (~30 s). Subsequent launches use"
echo "the cached model (loads in ~2 s). Android TTS is used as fallback while"
echo "the model is loading."
