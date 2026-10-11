# Sanjeevani Presentations

Two LaTeX Beamer presentations for the Sanjeevani Android emergency-skills training platform.

## Contents

```
sanjeevani-presentations/
├── pitch-deck/
│   ├── main.tex             — Complete pitch deck source (15 slides)
│   ├── references.bib       — Bibliography
│   ├── speaker-notes.md     — Full speaking script with timings
│   ├── assets/              — Place screenshots here
│   └── output/              — Compiled PDF appears here after build
├── system-design/
│   ├── main.tex             — Complete system design source (10 slides)
│   ├── references.bib       — Bibliography
│   ├── speaker-notes.md     — Technical speaking notes
│   ├── assets/              — Place screenshots here
│   └── output/              — Compiled PDF appears here after build
├── build.sh                 — Build both presentations
└── README.md                — This file
```

## Prerequisites

Install a TeX distribution that includes pdfLaTeX, TikZ, and Beamer:

**macOS (recommended):**
```bash
brew install --cask mactex
```

**Ubuntu / Debian:**
```bash
sudo apt-get install texlive-full latexmk
```

**Windows:** Install MiKTeX from https://miktex.org

Verify:
```bash
pdflatex --version
latexmk --version
```

## Building

### Build both presentations at once:
```bash
./build.sh
```

### Build individually:
```bash
# Pitch deck
cd pitch-deck
latexmk -pdf -interaction=nonstopmode main.tex
cp main.pdf output/Sanjeevani_Pitch_Deck.pdf

# System design
cd ../system-design
latexmk -pdf -interaction=nonstopmode main.tex
cp main.pdf output/Sanjeevani_System_Design.pdf
```

### Manual compile (if latexmk is unavailable):
```bash
pdflatex main.tex
pdflatex main.tex   # second pass for cross-references
```

## Adding Screenshots

Place actual app screenshots in the `assets/` folder of the relevant
presentation, then replace the TikZ phone schematics with:

```latex
\includegraphics[width=0.9\textwidth]{assets/screenshot_hand_placement.png}
```

The TikZ schematics on Slides 7 and 10 of the pitch deck are clearly labelled
"Schematic" — replace them when real screenshots are available.

## Design Notes

- **Colour palette:** Deep navy (#0D1B2A) background, teal (#0FA3B1) primary accent, orange (#F07A25) secondary accent
- **Implemented vs proposed:** Green `[impl]` label for verified features; dashed borders and orange `[proposed]` for planned features
- **Honest engineering:** Camera limitations (no depth, no force) are stated explicitly on multiple slides
- **Medical accuracy:** Content aligned with AHA 2020 and Resuscitation Council UK 2021 guidelines. Qualified clinical review is required before formal deployment.

## Slide Counts

| Presentation       | Slides | Duration  |
|--------------------|--------|-----------|
| Pitch deck         | 15     | 5–7 min   |
| System design      | 11     | 3–5 min   |

## Key Architectural Honesty Points

The presentations accurately reflect what is and is not implemented:

| Claim                               | Status              |
|-------------------------------------|---------------------|
| CPR guidance + AR hand correction   | Implemented         |
| Compression cycle counting          | Implemented (est.)  |
| Compression depth measurement       | NOT POSSIBLE (camera)|
| Stroke FAST face/arm detection      | Implemented (heuristic)|
| Allergy 6-phase + EpiPen simulation | Implemented         |
| AED / Choking / Recovery Position   | Listed; not implemented |
| On-device LLM                       | Code present; model not bundled |
| Backend API / cloud sync            | Not implemented (proposed) |
| Room database                       | Not used; SharedPreferences only |
| Clinical approval                   | Outstanding          |

## Sources

All citations in `references.bib`. Key references:
- American Heart Association CPR guidelines (2020)
- Resuscitation Council UK Adult BLS (2021)
- Indian bystander CPR research: PMID 28680517, 34474756, PMC10568059
- MediaPipe Solutions: ai.google.dev/edge/mediapipe
