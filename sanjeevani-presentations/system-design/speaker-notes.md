# Sanjeevani System Design — Speaker Notes

Target duration: **3–5 minutes**

---

## Cover (0:00)
Brief: *"Sanjeevani — how the system works. From what the user does to what the app records and how it decides what to show next."*

---

## Slide 1 — The Simplest Explanation (0:00–0:30)
*"Five blocks. User practises → app evaluates supported actions → feedback → progress records → personalised practice → back to the user."*

This is the whole product in one sentence. Every other slide is an expansion of one of these five blocks.

---

## Slide 2 — High-Level Architecture (0:30–1:30)
Walk top to bottom.

- **App layer**: Kotlin / Jetpack Compose. Single Activity. Four bottom-nav tabs.
- **Training Intelligence**: the FSM is the authority on workflow progression. Camera never overrides user answers for responsiveness or breathing.
- **Learning & Progress**: SharedPreferences for persistence — no database, no cloud.
- **Dashed boxes**: backend API, auth, and sync are proposed, not implemented. The LLM code is present but the model is not bundled.

Key point: everything working today is fully on-device, no internet required.

---

## Slide 3 — One Training Session (1:30–2:15)
*"The critical branch is the decision diamond: is there enough evidence to verify this action?"*

- Yes → record verified result
- No → show uncertainty, offer retry
- Manual continue → stored as manual, never as verified

*"This is where the system earns trust. It does not pretend the camera saw something it could not measure."*

---

## Slide 4 — Camera Feedback (2:15–3:00)
*"The pipeline: camera → frame processing → MediaPipe landmarks → measurement → rules → feedback."*

Four states:
1. **Verified**: confidence ≥ 65%, stable ≥ 1 second
2. **Detected but incorrect**: target found, wrong position → direction arrow shown
3. **Inconclusive**: low confidence or tracking lost → retry offered
4. **Unsupported**: depth, force, contact → explicitly not measured

*"A detected wrist position does not prove contact or pressure. The system knows this and does not claim otherwise."*

---

## Slide 5 — Shared Architecture, Separate Rules (3:00–3:35)
*"One engine, three assessment vocabularies."*

- CPR uses Pose (sternum target) + Hand (palm MCP landmarks)
- Stroke uses Face mesh (asymmetry ratio) + Pose (arm drift angle)
- Allergy uses Pose (patient lying, leg Y coordinate) + Hand (wrist proximity to thigh)

*"The shared FSM, feedback renderer, and event store mean adding a new module is mostly writing its assessment rules — not rebuilding the plumbing."*

---

## Slide 6 — How a Mistake Becomes Insight (3:35–4:10)
*"Important detail: one continuous positioning error = one error episode, not hundreds of camera-frame events. Analytics are meaningful because the data is clean."*

Storage: SharedPreferences only. Last 20 sessions per module. No Room database. No network. Session state is in-memory; it is lost if the app process is killed.

---

## Slide 7 — Targeted Practice (4:10–4:40)
*"The recommendation engine identifies the weakest criteria. Targeted practice re-enters the relevant drill — same camera tracking, same feedback engine, same assessment rules. No artificial distinction between 'drill' and 'training session'."*

---

## Slide 8 — Reducing Equipment Dependence (4:40–5:05)
*"The phone handles repetition, guidance, and awareness. The mannequin handles depth and force feedback. Both have a place."*

Be direct: Sanjeevani reduces the barrier to frequent revision. It does not eliminate the mannequin session for skills that require physical force measurement.

---

## Slide 9 — Data, Privacy, Reliability (5:05–5:35)
*"Everything is on-device. Camera frames are processed and discarded. No raw media is stored or transmitted. No internet permission used. SharedPreferences for persistence."*

Edge cases:
- Stale frames dropped by age gate
- Multi-person scenes treated as ambiguous
- Stale STT results blocked by TurnGate (epoch + FSM version pair)
- Process kill: in-memory session lost; stored events kept

---

## Slide 10 — The Whole System in One Picture (5:35–end)
*"Three statements. Training is interactive. Feedback is based on available evidence. Progress helps identify what to practise next."*

A non-technical judge should understand this in 20 seconds.
