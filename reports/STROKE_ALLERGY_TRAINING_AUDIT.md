# Sanjeevani Camera-Training Integration Audit

Date: 2026-10-11  
Branch: `feature/person-b-ui`  
Baseline inspected: `4aed31c`

## Result

Training Home, Practice, and focused Stroke/Allergy routes now open the original CameraX + MediaPipe + AR screen. The four-choice and microphone assessment is a collapsible panel over that live camera; it no longer replaces the camera workflow. CPR continues to use its existing live camera/FSM path.

The module timers were removed. Stroke and Allergy camera phases change only when the learner completes, retries, or manually advances the corresponding assessed training step. Entering any camera module resets the prior emergency FSM and explicitly selects the new module, preventing the previous “Done / emergency team taking over” state from leaking into a new session.

## Restored camera and AR paths

- CPR: CameraX analysis → MediaPipe Pose/Hands → `SanjeevaniEngine` spatial assessment → skeleton, sternum target, directional correction, position/hand/posture guidance, compression pulse/count UI.
- Stroke: live camera remains visible throughout FAST training. Face step uses Face Mesh mouth corners; arm step uses pose wrists/elbows; skeleton and tracking-state guidance remain visible. Four answer choices and voice input use the same deterministic assessment logic.
- Allergic Reaction: live camera remains visible. Pose hip/knee landmarks create an outer mid-thigh reference marker. The injector itself is not tracked; the placement exercise remains explicitly labelled a trainer/diagram simulation.

The assessment panel has **Hide / Show choices**, so it can collapse while the learner frames the relevant body part.

## Actual tracking capability

| Module | Genuinely observed | Error/availability states | Not claimed |
|---|---|---|---|
| CPR | Existing pose/hand spatial relationship, patient orientation, hand/arm posture, compression motion signals | directional left/right/up/down, tracking lost, not lying flat, posture/placement correction | clinically validated depth or force |
| Stroke face | Face Mesh mouth corners 61/291 normalized by eye width 33/263; at least five usable observations before an event | face outside frame, possible mouth-corner asymmetry, observed reference | stroke diagnosis or rule-out |
| Stroke arms | Pose elbows/wrists with visibility threshold; five agreeing frames required for an arm-height finding | both arms not visible, possible height difference | diagnosis from pose alone |
| Stroke speech | learner/button report only | recorded / not yet reported | camera-based speech assessment |
| Allergy thigh | right hip 24 to knee 26 midpoint when both are visible | reference visible / move hip and knee into frame | injector detection, orientation, or camera-verified injection |

One noisy Stroke arm frame cannot become a finding. Missing/low-confidence landmarks remain inconclusive and prompt reframing.

## Shared event and progression behavior

`ModuleTrainingAttempt` persists:

- IDs for attempt/session/module/step/skill/question
- event and assessment method (`BUTTON`, `VOICE`, `CAMERA_MEDIAPIPE`, `SIMULATED_PLACEMENT`, `MANUAL`)
- observation type and structured measurement summary
- answer, expected answer, knowledge result, outcome, error category, confidence and quality
- feedback, retry count, correction status/detected flag, manual-advance flag
- timestamp, content version, tracking model version, and provenance

Camera events are deduplicated per step/observation instead of saving every frame. A later assessed answer remains authoritative. Manual progression is tap-throttled, cancels voice work, persists `INCONCLUSIVE` with `manually_advanced_unconfirmed`, and never awards success.

Wrong choices persist immediately, show specific correction, and require retry. Both speech and buttons call the same assessment engine. Speech that is ambiguous or below confidence threshold does not guess.

## Analytics and personalized practice

- Stroke and Allergy analytics are calculated from persisted real events, separately from demo data.
- Skill cards show assessed errors, corrected attempts, inconclusive observations, manual advances, and latest state.
- Focus recommendations use canonical skill IDs and open the matching original camera module/step. They do not open a detached mini-quiz.
- Inconclusive camera observations do not reduce scored accuracy and do not award proficiency.
- The Practice tab exposes CPR, Stroke, and Allergy focused routes plus the optional knowledge quiz.

## Consumer UI

The stable bottom navigation now exposes Train, Practice, Analytics, and Profile. Profile has functional voice mute, live camera/microphone permission state, training history, local-data deletion, privacy behavior, and the medical-training disclaimer. Structured events are local; the training stores do not persist raw frames or audio.

## Files changed for this integration

- `MainActivity.kt`: restored camera routing, module-state reset/selection, hybrid panel wiring, stable navigation.
- `SanjeevaniViewModel.kt`: camera-step delegation and honest session/event boundaries.
- `engine/StrokeModule.kt`: manual phase control, actual Face Mesh indices, visibility/temporal checks, non-diagnostic status.
- `engine/AllergicReactionModule.kt`: manual phase control and honest thigh-reference guidance.
- `engine/SanjeevaniEngine.kt`: camera step selection and live tracking/availability status.
- `model/Models.kt`: camera observation counters in the snapshot.
- `ModuleTrainingViewModel.kt`: voice/button convergence, retries, immediate persistence, camera observations, manual progression, focused practice.
- `training/ModuleTrainingModels.kt`, `ModuleSkillStore.kt`: expanded backward-compatible event schema and aggregation.
- `ui/ModuleTrainingScreen.kt`: collapsible four-choice camera panel and manual path.
- `ui/ConsumerHubScreens.kt`: Practice and functional Profile/Settings screens.
- training/analytics/result screens and tests: persisted skill evidence and focused routes.

## Verification

Executed after integration:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

- Unit/Robolectric: **158 passed, 0 failed, 0 skipped**
- Android lint: **PASS**
- Debug APK: **PASS**
- Installed with `adb install -r`: **Success**
- Device: iQOO I2501 (`10BFC41SG2001UZ`)
- Device evidence: Stroke Start Training opened the live rear camera, MediaPipe/AR overlay, honest “Move face into camera frame” state, and the collapsible four-choice panel at the matching saved step. No fatal exception was logged.

## Honest limitations / remaining validation

- The Allergy injector is not visually detectable with the current Pose model. The camera supplies the thigh reference only; placement is a guided simulation.
- Stroke observations require clinical/content review and dataset validation before any sensitivity/specificity claim. The app correctly presents them as observations, not diagnosis.
- Existing CPR camera logic remains the most complete physical workflow, but the newer shared per-event schema is used by Stroke/Allergy; CPR still has its existing CPR skill/session store rather than a fully unified event table.
- The complete physical CPR mistake → correction → analytics journey and the complete Allergy trainer-simulation journey still need controlled human execution on device. Automated tests do not prove real-world landmark accuracy.
- Permission denial, camera contention, rotation/backgrounding, and a controlled A–D speech matrix require a dedicated device QA pass.

Medical reference points used for the content review: India ERSS 112, Stroke Association FAST resources, and Resuscitation Council UK first-aid guidance. This remains training software, not a diagnostic device or certification.
