#!/usr/bin/env bash
# download_llm.sh — Download Gemma 3 1B INT4 and push to device for SanjeevaniLLM
#
# Usage:
#   ./download_llm.sh                       # auto-detect connected device
#   ./download_llm.sh <device-serial>       # target a specific device
#
# Requirements:
#   - adb in PATH
#   - ~700 MB free in /tmp (or override TMPDIR)
#   - Device connected via USB with USB Debugging enabled
#
# The model is pushed to the app's internal files dir:
#   /data/data/com.sanjeevani/files/llm/model.bin
# SanjeevaniLLM.initialize() looks for it there on startup.

set -euo pipefail

PACKAGE="com.sanjeevani"
DEVICE="${1:-}"

# ── Model source ──────────────────────────────────────────────────────────────
# MediaPipe-compatible Gemma 3 1B INT4 .task bundle from Google AI Edge Gallery.
# This URL points to the official MediaPipe model on Kaggle/Hugging Face.
# If the URL changes, grab the latest from:
#   https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
MODEL_URL="https://huggingface.co/google/gemma-3-1b-it-mediapipe/resolve/main/gemma3-1b-it-q4.task"
MODEL_FILENAME="gemma3-1b-it-q4.task"
LOCAL_PATH="${TMPDIR:-/tmp}/${MODEL_FILENAME}"

# ── Helpers ───────────────────────────────────────────────────────────────────
ADB="adb"
[[ -n "$DEVICE" ]] && ADB="adb -s $DEVICE"

log() { echo "[llm-dl] $*"; }
die() { echo "[llm-dl] ERROR: $*" >&2; exit 1; }

# ── Pre-flight ────────────────────────────────────────────────────────────────
command -v adb >/dev/null 2>&1 || die "adb not found. Install Android Platform Tools."

# Wait for device
log "Waiting for device…"
$ADB wait-for-device

DEVICE_SERIAL=$($ADB get-serialno 2>/dev/null || echo "unknown")
log "Connected: $DEVICE_SERIAL"

# ── Download model ────────────────────────────────────────────────────────────
if [[ -f "$LOCAL_PATH" ]]; then
    log "Model already downloaded at $LOCAL_PATH — skipping download."
else
    log "Downloading $MODEL_FILENAME (~700 MB)…"
    log "URL: $MODEL_URL"
    curl -L --progress-bar \
         -H "Accept: application/octet-stream" \
         "$MODEL_URL" \
         -o "$LOCAL_PATH" \
    || die "Download failed. Check the URL or your internet connection."
    log "Download complete: $(du -h "$LOCAL_PATH" | cut -f1)"
fi

# ── Push to device ────────────────────────────────────────────────────────────
DEVICE_DIR="/data/data/${PACKAGE}/files/llm"
DEVICE_PATH="${DEVICE_DIR}/model.bin"

log "Creating directory on device: $DEVICE_DIR"
$ADB shell run-as "$PACKAGE" mkdir -p llm \
    || $ADB shell "mkdir -p '$DEVICE_DIR'" 2>/dev/null \
    || log "  (mkdir may have failed — directory may already exist)"

log "Pushing model to device (this may take a minute)…"
$ADB push "$LOCAL_PATH" "/sdcard/Download/${MODEL_FILENAME}"

log "Copying from sdcard to app files dir (run-as)…"
$ADB shell run-as "$PACKAGE" cp \
    "/sdcard/Download/${MODEL_FILENAME}" \
    "files/llm/model.bin"

# Clean up from sdcard
$ADB shell rm -f "/sdcard/Download/${MODEL_FILENAME}" 2>/dev/null || true

log "Verifying…"
SIZE=$($ADB shell run-as "$PACKAGE" stat files/llm/model.bin 2>/dev/null \
       | grep -i size | awk '{print $2}' || echo "unknown")
log "model.bin on device: $SIZE bytes"

log ""
log "Done! Restart the app — Sanjeevani will load the model on launch."
log "You should see '🤖 Loading AI model…' briefly in the top-left corner."
