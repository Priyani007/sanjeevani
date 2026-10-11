#!/usr/bin/env bash
# ============================================================
#  build.sh — Compile both Sanjeevani presentations
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()    { echo -e "${GREEN}[build]${NC} $*"; }
warn()   { echo -e "${YELLOW}[warn]${NC}  $*"; }
error()  { echo -e "${RED}[error]${NC} $*"; }

# ── Check for latexmk or pdflatex ─────────────────────────
if command -v latexmk &>/dev/null; then
  COMPILER="latexmk"
  log "Using latexmk"
elif command -v pdflatex &>/dev/null; then
  COMPILER="pdflatex"
  warn "latexmk not found — using pdflatex (two-pass)"
else
  error "Neither latexmk nor pdflatex found."
  error "Install TeX Live: brew install --cask mactex  (macOS)"
  error "                  sudo apt-get install texlive-full  (Debian/Ubuntu)"
  exit 1
fi

compile() {
  local dir="$1"
  local name="$2"
  local out_name="$3"

  log "Building $name..."
  pushd "$dir" > /dev/null

  if [[ "$COMPILER" == "latexmk" ]]; then
    latexmk -pdf -interaction=nonstopmode -halt-on-error main.tex
  else
    pdflatex -interaction=nonstopmode main.tex
    pdflatex -interaction=nonstopmode main.tex
  fi

  if [[ -f "main.pdf" ]]; then
    mkdir -p output
    cp main.pdf "output/${out_name}.pdf"
    log "  → output/${out_name}.pdf"
  else
    error "$name compilation failed — no PDF produced."
    popd > /dev/null
    return 1
  fi

  # clean up aux files
  if [[ "$COMPILER" == "latexmk" ]]; then
    latexmk -c &>/dev/null || true
  else
    rm -f main.aux main.log main.out main.nav main.snm main.toc main.vrb main.fdb_latexmk main.fls
  fi

  popd > /dev/null
}

# ── Build both ─────────────────────────────────────────────
compile "pitch-deck"    "Pitch Deck"    "Sanjeevani_Pitch_Deck"
compile "system-design" "System Design" "Sanjeevani_System_Design"

log ""
log "Build complete."
log "  pitch-deck/output/Sanjeevani_Pitch_Deck.pdf"
log "  system-design/output/Sanjeevani_System_Design.pdf"

# ── Optional: create ZIP archive ──────────────────────────
if command -v zip &>/dev/null; then
  log ""
  log "Creating ZIP archive..."
  ZIP_NAME="Sanjeevani_Presentations_$(date +%Y%m%d).zip"
  zip -r "$ZIP_NAME" \
    pitch-deck/main.tex pitch-deck/references.bib pitch-deck/speaker-notes.md \
    "pitch-deck/output/Sanjeevani_Pitch_Deck.pdf" \
    system-design/main.tex system-design/references.bib system-design/speaker-notes.md \
    "system-design/output/Sanjeevani_System_Design.pdf" \
    README.md build.sh \
    2>/dev/null || warn "ZIP creation had warnings (likely missing optional assets)"
  log "  → $ZIP_NAME"
fi
