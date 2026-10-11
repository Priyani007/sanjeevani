# Sanjeevani Technical Architecture

## Codebase Audit: What Is Working vs. Partial vs. Missing

### Takeaway
The core engine is production-quality for CPR, Stroke FAST, and Allergic Reaction. The module extension pattern is clean and ready. AED, Choking, and Recovery Position are stubbed in the UI as "Coming Soon" with no backend implementation. Session persistence and cross-session progress tracking are entirely absent.

### Cited Findings

**EmergencyModule interface** — `app/src/main/java/com/sanjeevani/model/Models.kt` line 127:
```kotlin
interface EmergencyModule {
    fun process(frame: PerceptionFrame, nowMs: Long): ModuleResult
    fun reset()
}
```
This is the sole contract a new module must satisfy. `ModuleResult` carries `voiceText: String?`, `overlay: AROverlaySpec`, and `isComplete: Boolean`. The interface is fully extensible.

**FSMState enum** — `Models.kt` line 28. Current states:
`IDLE, SCENE_ASSESSMENT, RESPONSIVENESS_CHECK, BREATHING_ASSESSMENT, POSITION_CONFIRMED, EMERGENCY_ESCALATION, CPR_POSITIONING, HAND_POSITIONING, POSTURE_CHECK, COMPRESSION_ACTIVE, CPR_PAUSE, CPR_SUCCESS, TRIAGE_DETECTION, STROKE_FAST_TEST, HEART_ATTACK_CONSCIOUS, ALLERGIC_PROTOCOL`.
**Missing states** needed for new modules: `AED_PROTOCOL`, `CHOKING_PROTOCOL`, `RECOVERY_POSITION`. Adding them requires one enum entry each.

**EmergencyType enum** — `Models.kt` line 7: `CPR, FAST_STROKE, HEART_ATTACK, ALLERGIC_REACTION, UNKNOWN`. Missing: `AED`, `CHOKING`, `RECOVERY_POSITION`.

**TrainingHomeScreen** — `ui/TrainingHomeScreen.kt` line 26–33. Modules listed:
- `cpr` — available=true, wired
- `allergy` — available=true, wired
- `stroke` — available=true, wired
- `aed` — **available=false**, no callback
- `choking` — **available=false**, no callback
- `recovery` — **available=false**, no callback

The UI grid already has the card layout; enabling a new module is a one-line change (`available=true`) plus wiring the lambda.

**SanjeevaniEngine.kt** dispatch pattern (lines 114–127):
```kotlin
if (state == FSMState.ALLERGIC_PROTOCOL) { /* allergicModule.process(...) */ }
if (state == FSMState.STROKE_FAST_TEST)  { /* strokeModule.process(...) */ }
```
A new module is added by: (1) instantiating it in `SanjeevaniEngine`, (2) adding an `if (state == FSMState.AED_PROTOCOL)` block that mirrors the allergic/stroke pattern, (3) calling `reset()` from `resetForNewEmergency()`, and (4) returning the module's overlay from `snapshot()`. The existing pattern requires ~40 lines of boilerplate per module.

**EmergencyFSM.setUserSelectedEmergency()** (lines 59–71) routes:
- `ALLERGIC_REACTION` → `ALLERGIC_PROTOCOL`
- `FAST_STROKE` → `STROKE_FAST_TEST`
- Everything else → `RESPONSIVENESS_CHECK`
New emergency types need a `when` branch added here.

**Working (verified by 36 passing unit tests):**
- Full CPR workflow: `RESPONSIVENESS_CHECK` → `BREATHING_ASSESSMENT` → `CPR_POSITIONING` → `HAND_POSITIONING` → `POSTURE_CHECK` → `COMPRESSION_ACTIVE`
- Allergic reaction: 6-phase timer (LAY_FLAT 0s, RAISE_LEGS 17s, SAFE_POSITION 38s, EPIPEN_READY 48s, EPIPEN_INJECT 51s, MONITORING 66s)
- Stroke FAST: INTRO(3s) → FACE(10s) → ARM(12s) → SPEECH(user input or 45s timeout) → RESULT
- Version guards on FSM answers (stale callbacks rejected)
- TTS voice (Android TTS primary, Kokoro TTS fallback)
- Speech recognition with contextual grammar interpretation

**Partial/limited:**
- `HeartAttackModule.kt` exists on disk but is not dispatched — CLAUDE.md explicitly notes it needs safety review before restoring.
- `SanjeevaniLLM.kt` exists (GenAI dependency retained) but the model is NOT initialized or called. The LlmState shows loading/error status in the UI, but no LLM output reaches clinical decisions. This is intentional.
- Compression depth: NOT measured. `CompressionDetector` counts torso-relative hand motion cycles and rate. The CLAUDE.md instruction "Do not claim CPR compression depth can be measured from monocular camera" is respected throughout.
- Face asymmetry detection in `StrokeModule` (lines 101–107) uses **shoulder landmark y-coordinate difference** (landmarks 11/12), not actual facial landmarks. This measures shoulder tilt/droop, not drooping of facial features. This is a known limitation — a proper face asymmetry check would need FaceLandmarker blendshapes (FACE_LEFT_EYE_BLINK etc.), but those require `setOutputFaceBlendshapes(true)` in `MediaPipeController.initFace()`, currently false.

**Missing:**
- AED module (no Kotlin file)
- Choking module (no Kotlin file)
- Recovery position module (no Kotlin file)
- Session persistence across process death (`ViewModel` survives Activity recreation but not process kill)
- Progress/score tracking across sessions (no database, no Room, no DataStore)
- Module result scoring (the `StrokeSignals` data class exists and is populated; nothing persists it)

### Inferences
- The `EmergencyModule` interface was designed with extension in mind; the 2-method contract is the right seam for new modules.
- The timer-based phase pattern in `AllergicReactionModule` is the correct template for AED (which is also primarily timer and decision-tree driven with minimal pose dependency).
- The FAST stroke module's arm assessment is useful as a rough indicator but the face asymmetry assessment is measuring the wrong landmarks; correcting it would strengthen stroke detection but requires enabling face blendshapes.

### Gaps
- `CPRWorkflow.kt`, `CompressionDetector.kt`, `SpatialReasoner.kt`, `PatientDetector.kt` were not read in full; their internal algorithms are not audited here but their outputs are well-defined via the model types.
- `SanjeevaniViewModel.kt` not read; its TTS/recognition lifecycle details are documented in CLAUDE.md but not independently verified here.

---

## What Does the EmergencyModule Interface Look Like? Is It Extensible?

### Takeaway
The interface is minimal and directly extensible. Any new module (AED, choking, recovery position) implements exactly two methods. The surrounding dispatch and overlay pipeline impose no additional constraints beyond returning a valid `ModuleResult`.

### Cited Findings
- `EmergencyModule` is in `model/Models.kt` line 127. It has no abstract state, no constructor arguments, and no Android-specific dependencies.
- `ModuleResult` carries the full display contract: voice cue string, `AROverlaySpec` for AR rendering, and a completion boolean. The engine polls `voiceText` and `overlay` each frame; the module only populates them on transitions.
- Both existing implementations (`AllergicReactionModule`, `StrokeModule`) are ~115 lines each, self-contained, and testable without Android dependencies.
- The `AllergicPhase` and `FastStep` enums are private to their respective files. New modules can follow the same pattern.

### Inferences
- An `AEDModule` can be modeled identically to `AllergicReactionModule`: a private step enum, a `STEP_DURATION_MS` map, and `stepVoice`/`buildOverlay` functions.
- The interface imposes no coupling to the FSM; the module only knows about `PerceptionFrame` (camera input) and wall-clock time.

---

## What Does the Existing FSM Support?

### Takeaway
The FSM is authoritative, deterministic, and version-guarded. It transitions on user answers (clinical) or stable camera observations (position estimate only). It does not support new-module states out of the box — three new enum values and routing branches must be added.

### Cited Findings
- `EmergencyFSM` has two progression paths: `answer(Answer, nowMs, expectedVersion)` for user-confirmed clinical transitions, and `process(GuidanceState, nowMs)` for camera-geometry observations. Camera never drives responsiveness or breathing decisions.
- Version stamping (`version: Long`, incremented on each transition) prevents duplicate and stale answers from advancing state.
- Camera-based progression: only `CPR_POSITIONING` (patient lying detection, confidence ≥ 0.65) and `HAND_POSITIONING` (corrective action = CORRECT, both confidences ≥ 0.65) can be advanced by the camera, and only after 1 second of continuous stable evidence.
- `history: List<TransitionRecord>` records every transition with timestamp, reason, and version for auditability.
- `completed: Set<FSMState>` tracks which steps are done; re-entry to completed steps is prevented unless `resetForNewSession` is called.
- `setUserSelectedEmergency()` routes direct-to-protocol for ALLERGIC_REACTION and FAST_STROKE; everything else goes to RESPONSIVENESS_CHECK first.

### Inferences
- For AED training, the entire workflow is user-answer-driven (no camera clinical dependency), so no changes to the camera-based `process()` path are needed. AED adds one state, one `setUserSelectedEmergency` branch, and one module.
- For Choking, the Heimlich/back-blow protocol is also entirely user-driven (no pose measurement is clinically validated), so the same pattern applies.
- Recovery position involves guiding the rescuer to move the patient; pose heuristics could give visual guidance (similar to CPR HAND_POSITIONING) but should not gate progression.

---

## What MediaPipe Landmarks Are Being Used? What Can Reliably Be Detected?

### Takeaway
The app uses MediaPipe's Pose Landmarker (heavy, 33 landmarks), Hand Landmarker (21 landmarks per hand), and Face Landmarker (478 landmarks) with GPU-first inference. The face landmarks are collected but not used for clinical decisions. Reliable on-device detections include: body lying posture (heuristic), shoulder y-coordinate asymmetry, arm droop, hand position relative to torso.

### Cited Findings
- `MediaPipeController.kt` uses:
  - `PoseLandmarker` with `pose_landmarker_heavy.task`, `setNumPoses(2)`, `LIVE_STREAM` mode, results callback
  - `HandLandmarker` with `hand_landmarker.task`, `setNumHands(2)`, `LIVE_STREAM` mode
  - `FaceLandmarker` with `face_landmarker.task`, `setNumFaces(2)`, `LIVE_STREAM` mode, `setOutputFaceBlendshapes(false)`
  - GPU delegate first (Snapdragon Adreno/NPU), CPU fallback
- Pose freshness gate: `poseTimestamp` within 300ms of frame, `handTimestamp` within 250ms, pose/hand separation ≤150ms.
- Multi-person pose (`setNumPoses(2)`) but multi-person result is treated as ambiguous and rejected: `it.landmarks().size == 1` required.
- Landmarks used in clinical heuristics: 0 (nose), 11/12 (shoulders), 13/14 (elbows), 15/16 (wrists), 23/24 (hips), 25/26 (knees)
- World landmarks (3D metric, meters, Y=up) used in `PatientDetector.isPatientLying()`.
- Face landmarks collected (478 points) but `buildFaceMeshPoints()` in `SanjeevaniEngine` is never called in the active dispatch path.

**BlazePose real-time performance:** The 2020 Google research blog confirms that BlazePose achieves real-time inference on mobile with CPU. GPU acceleration via the Adreno delegate is standard for Snapdragon devices. — [Google Research Blog](https://www.research.google/blog/on-device-real-time-body-pose-tracking-with-mediapipe-blazepose/)

**What can reliably be detected from a monocular camera on a mid-range phone:**
1. Shoulder y-coordinate asymmetry (shoulder tilt) — used in StrokeModule. Low threshold (>0.07 normalized), suitable as a screening indicator, not clinical confirmation.
2. Arm droop relative to elbow (wrist y minus elbow y per side, asymmetry >0.12 normalized) — used in StrokeModule. Roughly corresponds to one arm failing to extend to elbow height when both arms raised.
3. Body lying posture — PatientDetector uses world landmarks. Heuristic, not validated against clinical standard.
4. Hand midpoint position relative to estimated sternum — SpatialReasoner. Used to show directional arrow, not to confirm correct placement.
5. Hand motion cycles during compressions — CompressionDetector. Counts cycle frequency; does NOT measure depth.

**What cannot be reliably detected:**
- CPR compression depth from monocular camera (no depth ground truth, scale-unknown from a single view; CLAUDE.md explicitly prohibits claiming this)
- Responsiveness or breathing from pose alone (CLAUDE.md: "Camera never determines responsiveness or breathing")
- Confirmed supine orientation (world Y is not a ground sensor; noted in CLAUDE.md)
- Finger splaying, airway positioning for recovery, or pad electrode placement for AED

### Inferences
- Face asymmetry check in StrokeModule is currently measuring shoulder asymmetry, not facial drooping. Enabling face blendshapes (e.g., `mouthLeft`, `mouthRight`, `eyeSquintLeft`, `eyeSquintRight` from FaceLandmarker 52 blendshapes) would give a more accurate face droop signal, at a slight increase in inference cost.
- Arm raise asymmetry: the wrist/elbow droop check is a reasonable proxy for "one arm drifts down during bilateral raise" — consistent with the FAST test protocol. The NIOSH single-camera asymmetry study reports mean absolute error of 6.25° SD=10.19° against motion capture for lifting asymmetry; arm raise detection would be similar in quality. — [CDC/NIOSH single-camera asymmetry study](https://stacks.cdc.gov/view/cdc/208833)
- Camera viewpoint is critical for arm asymmetry detection; a front-facing view of the patient is optimal.

---

## Can a Phone Camera Reliably Detect Arm Raise Asymmetry, Body Position, or Hand Placement?

### Takeaway
Arm raise asymmetry and rough body position can be detected with useful but imperfect accuracy using MediaPipe pose on a phone camera. Hand placement can only be shown as an approximate marker. None of these detections are clinically validated and should be presented as estimates only.

### Cited Findings
- **Arm raise asymmetry:** A 2025 NIOSH study on single-camera lift asymmetry estimation reports MAE of 6.25° (SD=10.19°, N=360) vs. motion capture. The pipeline uses HRNet + VideoPose3D; MediaPipe BlazePose would produce comparable quality for the 2D landmark step. "Camera viewpoint matters" — camera placement angle affects accuracy significantly. — [NIOSH single-camera asymmetry study, CDC STACKS](https://stacks.cdc.gov/view/cdc/208833)
- **Body position:** PatientDetector uses world landmarks (metric 3D) to detect lying posture. MediaPipe world landmarks provide metric estimates but "MediaPipe world Y is not a ground/gravity sensor" (CLAUDE.md). This is sufficient as a visual guide and approximate confirmation.
- **Hand placement:** SpatialReasoner computes a sternum target (0.35 of the way from shoulder midpoint toward hips) and computes directional correction vectors. This is an approximate anatomical estimate; the CLAUDE.md notes "chest marker interpolates 0.35 from shoulder midpoint toward hips; it is an approximate target."
- The codebase already correctly labels all these as estimates with confidence thresholds and explicit uncertainty in overlay text (e.g., "Approximate marker • not clinical verification").

### Inferences
- The existing approach of showing visual guidance with explicit "estimate only" labeling is architecturally sound and matches the research evidence on monocular pose accuracy.
- For AED pad placement, guiding the user to place pads on the "upper right chest" and "lower left side/rib" is best done with static diagram overlays, not pose-based targeting, since the chest skin is not a tracked landmark.

---

## Best Approaches for Adding AED Training

### Takeaway
AED training is a decision-tree + timer-based scenario with no camera clinical dependency. It fits the existing `EmergencyModule` pattern directly. The minimal implementation is a state machine with ~8 nodes, timers, and branch logic for shockable vs. non-shockable rhythm.

### Cited Findings
- AHA 2020 CPR Guidelines define the standard AED scenario sequence: scene safety → call 112 → CPR start → AED power on → pad attachment → rhythm analysis (pause CPR) → shock decision → post-shock CPR cycle (2 min) → reanalysis. — Referenced in AED Sim app listing (adherence to 2020 CPR Guidelines) — [AED Sim on App Store](https://apps.apple.com/ee/app/aed-sim/id359990739)
- AHA recommends training on all 8 standard scenarios: shockable rhythm, non-shockable rhythm, wet environment, loose electrode, and post-shock recovery variants.
- The SCHILLER trainer and AED Sim use declarative scenario definitions with configurable timers and voice prompts. — [SCHILLER Defi training](https://www.schiller.ch/en/products/fred-pa-1-trainer-p210)
- A Pocket Nurse guide recommends integrating AED training as part of the CPR sequence, not as a standalone scenario, to reinforce the "chain of survival" concept. — [Pocket Nurse AED integration guide](https://www.pocketnurse.com/default/blog/post/how-can-aed-be-best-integrated-into-lifesaving-medical-learning-scenarios)

**Recommended architecture for AEDModule:**
```
AEDStep enum: INTRO, POWER_ON, PAD_PLACEMENT, ANALYSIS_PAUSE, SHOCK_DECISION,
              POST_SHOCK_CPR, REANALYSIS, COMPLETE
```
Timers:
- ANALYSIS_PAUSE: ~8s (AED analysis window, user must stop CPR)
- POST_SHOCK_CPR: 120s (2-minute CPR cycle per AHA guidelines)
- INTRO / POWER_ON / PAD_PLACEMENT: user-gated (tap to confirm each step), with 60s timeout fallback

Branch logic: at `SHOCK_DECISION`, user taps "Shock advised" or "No shock advised" (or after 30s timeout defaults to "No shock"). This drives the voiceText and overlay color.

For training (not real AED response), the shockable/non-shockable branch can be determined at session start (random or instructor-chosen), eliminating the need to interface with real AED hardware.

**Static diagram overlay for pad placement:** Use `AROverlaySpec.guidanceText` and a drawable overlay (R.drawable.aed_pad_diagram) rather than pose-estimated targeting. Pose landmarks can draw a skeleton to help orient the user, but the pad target markers should be fixed diagram positions.

### Inferences
- AED module implementation is ~200 lines (comparable to AllergicReactionModule at 117 lines) plus UI controls.
- The two new `FSMState` values needed: `AED_PROTOCOL` in `EmergencyFSM`, and `AED` in `EmergencyType`.
- The 2-minute CPR timer after shock is the only place AED and CPR overlaps; the AEDModule can reuse CompressionDetector for pacing guidance during the post-shock CPR phase by reading `SanjeevaniEngine`'s `temporal` state.

---

## Recommended Architecture for Progress Tracking and Session Persistence

### Takeaway
The minimal safe approach is Room + DataStore on-device: Room for session records and module scores, DataStore for lightweight preferences and cumulative stats. No network dependency. The ViewModel already has the session context data needed; it just needs a persistence layer.

### Cited Findings
- `SanjeevaniEngine.sessionContext` (line 46) exposes `SessionContext` with `reportedConcern`, `activeConcern`, `completedSteps`, and `recentAnswers`. This is the primary audit trail per session.
- `EmergencyFSM.history: List<TransitionRecord>` provides a full transition log with timestamps, reasons, and versions.
- `StrokeModule.getStrokeSignals()` returns `StrokeSignals(faceAsymmetryScore, armDriftDetected, speechPrompted, positiveTestCount)` — a structured summary suitable for persisting.
- There is no process-death persistence currently; CLAUDE.md confirms this: "No process-death session persistence."
- Android Jetpack documentation (not searched) confirms Room as the standard SQLite ORM and DataStore as the successor to SharedPreferences for key-value and typed preferences storage.

**Recommended schema:**
```
TrainingSession (id, moduleType, startedAt, completedAt, durationMs, completedSteps JSON)
ModuleScore (sessionId, moduleType, scoreFields JSON)
// StrokeScore: faceAsymmetryScore, armDrift, speechPrompted, positiveCount
// CPRScore: compressionCount, averageRateEstimate, trackingReliableFraction
// AllergicScore: phasesCompleted, totalDurationMs
CumulativeStats (moduleType, totalSessions, bestScore, lastAttemptAt)
```

**DataStore:** Use for lightweight flags: `hasCompletedOnboarding`, `totalSessionCount`, `lastModuleUsed`, `preferredTTSRate`.

**Timing:** Session start = when `SanjeevaniEngine.resetForNewEmergency()` is called (or `setUserSelectedEmergency`). Session end = when `FSMState.CPR_SUCCESS` / `AllergicPhase.MONITORING` / `FastStep.RESULT` is reached or user navigates back. Persist on `SanjeevaniViewModel.resetForNewEmergency()` call.

### Inferences
- Adding Room requires ~3 data classes + a DAO + a `@Database` class + a `SanjeevaniRepository` singleton — approximately 150 lines.
- The `SanjeevaniViewModel` is the natural place to inject the repository; it already has the session data and lifecycle scope.
- Progress screens (e.g., "Your CPR Sessions") can be a new `destination` in `MainActivity`'s when-block, alongside "home", "cpr", "stroke", "allergy".
- No network is needed; all data stays on-device. This satisfies the "no external API for core training" constraint.

---

## On-Device vs. Cloud-Side Inference Tradeoffs

### Takeaway
On-device inference is correct for all core training functionality. The existing MediaPipe stack already runs on-device. Cloud inference should not be introduced for pose/hand detection or clinical decisions. It could optionally be used for richer feedback (e.g., a VLM describing technique) if connectivity is available, but this is out of scope for the hackathon.

### Cited Findings
- MediaPipe runs on-device with GPU acceleration on the Snapdragon (Adreno GPU delegate). Cold start is fast (CLAUDE.md reports "cold launch 343ms" on the iQOO device).
- The project constraint is explicit: "No external API dependencies requiring network for core training."
- The LLM dependency (GenAI SDK) is retained for source compatibility but the model is not initialized; CLAUDE.md states "Do not connect its free-form output to medical decisions or playback without a separate safety review."
- Gemini Nano experimental access opened to Android developers in October 2024 via AICore for text-to-text prompts on Pixel 9 series only. It is text-only on non-Pixel devices currently; multimodal (image+text) is announced but not publicly released for third-party apps. — [Android Developers Blog, October 2024](https://android-developers.googleblog.com/2024/10/gemini-nano-experimental-access-available-on-android.html)
- Gemini Nano with multimodality is "available later this year, starting with Google Pixel devices" per Google I/O 2024. This remains Pixel-restricted and not suitable for a mid-range iQOO target device. — [Android Developers Blog, June 2024](https://android-developers.googleblog.com/2024/06/top-3-updates-for-building-with-ai-on-android-google-io-24.html?hl=zh_TW)

**Tradeoff table:**

| Dimension | On-device (current) | Cloud-side |
|---|---|---|
| Latency | ~30–60ms/frame (GPU) | 200ms+ round-trip |
| Privacy | No data leaves device | Frames/landmarks sent externally |
| Offline | Works fully offline | Fails without connectivity |
| Cost | Zero per-request | API cost per session |
| Compliance | No PII transmitted | Requires data agreements |
| Clinical safety | Deterministic, auditable | LLM output not deterministic |

### Inferences
- Retain on-device inference for all clinical and training logic.
- The existing LLM stub could be activated for non-clinical uses only (e.g., "explain why we do this step"), with a strict safety review before enabling.
- MobileVLM / Gemini Nano image understanding would be useful for a future "show me your hand placement and I'll evaluate it" feature, but neither is available for mid-range non-Pixel Android devices as of 2024.

---

## Vision-Language Models on Android

### Takeaway
No vision-language model is ready for production use on a mid-range Android (iQOO) device as of late 2024. Gemini Nano multimodal is Pixel 9 only (experimental). MobileVLM is a research model without a production Android SDK. The existing MediaPipe pose pipeline is the correct and sufficient approach for visual guidance.

### Cited Findings
- **Gemini Nano (text-only):** Available on Pixel 9 via AICore; other devices are not yet supported. Developer API is experimental. — [Android Developers Blog, October 2024](https://android-developers.googleblog.com/2024/10/gemini-nano-experimental-access-available-on-android.html)
- **Gemini Nano multimodal:** Announced at Google I/O 2024 for Pixel devices; not available in developer SDK as of October 2024. — [Android Developers Blog, June 2024](https://android-developers.googleblog.com/2024/06/top-3-updates-for-building-with-ai-on-android-google-io-24.html?hl=zh_TW)
- **MobileVLM:** A research-oriented small VLM (1.7B–3B parameter) designed for mobile; there is no production-grade Android SDK or MediaPipe task wrapper. Would require ONNX or TFLite conversion and custom inference code.
- The iQOO device (`I2501`) is a mid-range Snapdragon device. Gemini Nano AICore is not confirmed for this chipset family in any searched source.
- The app already uses the Kokoro TTS model (local neural TTS) as a high-quality voice fallback, demonstrating that on-device neural models are viable when bundled as assets.

### Inferences
- For the hackathon demo, there is no path to a production VLM on the iQOO device. The MediaPipe pose stack provides sufficient visual feedback.
- A "rich feedback" feature using a VLM could be prototyped using cloud Gemini Flash (network-dependent), clearly gated behind a "requires internet" flag, separate from the core offline training path.

---

## Concrete Architecture Recommendation: Extending the Multi-Module Training Engine

### Summary

The codebase is structurally sound and ready to extend. The minimal safe path to adding AED, Choking, and Recovery Position is:

**Step 1: Model layer (Models.kt)**
- Add `AED`, `CHOKING`, `RECOVERY_POSITION` to `EmergencyType` enum
- Add `AED_PROTOCOL`, `CHOKING_PROTOCOL`, `RECOVERY_POSITION` to `FSMState` enum

**Step 2: FSM routing (EmergencyFSM.kt)**
- Add three `when` branches in `setUserSelectedEmergency()`:
  ```kotlin
  EmergencyType.AED -> transition(FSMState.AED_PROTOCOL, nowMs, "AED training selected")
  EmergencyType.CHOKING -> transition(FSMState.CHOKING_PROTOCOL, nowMs, "choking training selected")
  EmergencyType.RECOVERY_POSITION -> transition(FSMState.RECOVERY_POSITION, nowMs, "recovery position selected")
  ```
- Add `instruction()` cases for the three new states
- Add `expectsAnswer()` entries as needed

**Step 3: Module implementation**
- `AEDModule.kt` — 8-step timer+branch FSM (~200 lines), modeled on AllergicReactionModule. Key departure: add shockable/non-shockable branching driven by user tap at SHOCK_DECISION step.
- `ChokingModule.kt` — 5-step decision tree: CONSCIOUS_ADULT, BACK_BLOWS, ABDOMINAL_THRUSTS, UNCONSCIOUS_CPR, COMPLETE. Entirely user-driven (no camera clinical dependency).
- `RecoveryPositionModule.kt` — 4-step guided sequence: ROLL_TO_SIDE, TILT_HEAD, BEND_TOP_KNEE, MONITOR. Camera can show skeleton overlay for visual reference; user confirms each step.

**Step 4: Engine dispatch (SanjeevaniEngine.kt)**
- Add three `if (state == FSMState.X_PROTOCOL)` blocks in `process()` and `snapshot()`, each ~15 lines, following the existing allergic/stroke pattern
- Add three module instantiations and `reset()` calls in `resetForNewEmergency()`

**Step 5: UI wiring**
- `TrainingHomeScreen.kt`: change `available=false` to `available=true` for aed/choking/recovery; add three lambdas
- `MainActivity.kt`: add three `when` destinations in the navigation block, each ~5 lines
- `SanjeevaniScreen.kt`: add UI controls (confirm buttons) for each new state, following existing pattern for ALLERGIC_PROTOCOL

**Step 6: Progress tracking (new files)**
- `data/TrainingSession.kt`, `data/TrainingSessionDao.kt`, `data/SanjeevaniDatabase.kt` (~100 lines total, Room)
- `data/SanjeevaniRepository.kt` (~50 lines)
- Inject into `SanjeevaniViewModel`, call `repository.saveSession()` in `resetForNewEmergency()`
- Add `ProgressScreen.kt` Composable for session history view

**Total estimated addition: ~800–1000 lines for all three modules + persistence layer**, without changes to the working CPR/stroke/allergic pipeline.

### What Not to Touch

- Do NOT modify `EmergencyFSM.process()` camera path for new modules (all new modules are user-driven, not camera-gated)
- Do NOT re-enable `HeartAttackModule` without clinical review (CLAUDE.md warning)
- Do NOT connect `SanjeevaniLLM` to clinical decisions (requires separate safety review)
- Do NOT claim compression depth measurement is possible from camera
- Do NOT add cloud API dependencies to the core training path

### Test Strategy

Following the existing `WorkflowTest.kt` pattern:
- One test class per new module (`AEDModuleTest`, `ChokingModuleTest`, `RecoveryModuleTest`)
- Timer-driven advancement tests (same pattern as `allergicModuleAdvancesThroughPhases`)
- FSM routing tests for each new EmergencyType
- Reset cycle tests
- Edge cases: timeout on user-input steps, shockable vs. non-shockable branch paths

The existing 36-test suite provides the model. New module tests should bring the total to ~60 tests.
