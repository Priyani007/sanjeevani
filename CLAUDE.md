# Sanjeevani — Claude handoff

Updated 10 October 2026. Read this file before changing the app, then read
[the implementation and verification report](docs/SESSION_VERIFICATION.md).

## Current task context

The user requested an end-to-end repair of the emergency coach: one authoritative
workflow, contextual voice turn-taking, evidence-based position/hand feedback,
real motion-cycle estimates, and protection against stale callbacks and reopened
completed steps. This has been implemented on `feature/person-b-ui`.

The old B1–B5 tasks in AGENTS.md describe an earlier project phase, not current
implementation status. In particular, do not reintroduce timer-driven CPR,
camera-derived consciousness, or automatic CPR on a reported heart attack.
The latest end-to-end user brief explicitly required cross-layer engine/model/UI
changes; older branch ownership notes alone do not describe that work.

## Stack and entry points

Android Kotlin 2.0.21, Jetpack Compose, CameraX, MediaPipe; package
`com.sanjeevani`, minSdk 26, compileSdk 35, AGP 8.7.0.

- `MainActivity.kt`: preview, permission requests, lifecycle, answer controls,
  112 dialer, dispatcher mute.
- `SanjeevaniViewModel.kt`: serial state publication, TTS/recognition turn lifecycle,
  token/version guards, dedicated single-thread vision executor.
- `engine/EmergencyFSM.kt`: sole progression authority; guarded answers and
  stable visual observations; completed history and versioned transition records.
- `engine/SanjeevaniEngine.kt`: perception freshness, current-step validation,
  derived overlay, speech priority, structured bounded session context.
- `engine/CPRWorkflow.kt`: read-only state-to-step projection. It has no timers.
- `voice/SessionDialogue.kt`: conservative English final-transcript interpretation,
  contextual help and TurnGate. No runtime LLM decisions.
- `model/Models.kt`: source timestamps, FSM version, explicit overlay state,
  confirmation event and count reliability.

## Workflow invariants

Opening/selection → responsiveness.
Responsive → monitoring, NOT CPR.
Unresponsive → normal-breathing assessment.
Normal breathing → monitoring.
Reported unresponsive + not breathing normally → positioning → hand placement
→ explicit posture/readiness confirmation → active CPR coaching.

Stable camera position estimates lead to POSITION_CONFIRMED, where the user must
confirm actual back position/readiness. Manual positioning and hand-placement
fallbacks are explicitly user evidence, not camera verification.

Only current-step validation advances the FSM. Missing tracking during active
CPR never reopens positioning. Explicit changed-condition reports trigger a
documented reassessment; explicit emergency-team takeover ends app guidance.
Every transition records source, destination, timestamp, version and reason.
Duplicate answers and stale callbacks cannot advance a newer state.

## Voice and latency

Android TTS is primary for low startup latency (rate 1.05; full per-utterance gain,
system volume respected). Kokoro is a fallback, with serialized synthesis and
playback-head completion. Vision model loading does not block opening speech.

Question → successful speech completion → recognizer → final result →
contextual interpretation → guarded transition → next instruction.
No partial-transcript clinical decisions, arbitrary two-second auto-listen delay,
or speech/listening overlap by design. Listen status comes from onReadyForSpeech.
Silence/error leaves retry/tap controls. Recognition can depend on the installed
service and network. Audio focus loss, app pause, interruption and dispatcher mute
invalidate obsolete turns.

The existing `llm/SanjeevaniLLM.kt` and GenAI dependency are retained for source
compatibility (the UI uses LlmState), but the model is NOT initialized or called.
Do not connect its free-form output to medical decisions or playback without a
separate safety review. Legacy ContextAwareVoiceCompanion is also not the active
session dialogue implementation.

## Camera, estimates and visual feedback

Back camera only: bitmap is rotated upright before inference. OverlayCoordinates
maps normalized upright landmarks to FIT_CENTER preview coordinates. Spoken
directions mean SCREEN left/right/up/down, not anatomical directions.

Source pose age ≤300 ms; hand age ≤250 ms; pose/hand separation ≤150 ms.
Repeated source samples cannot accumulate evidence/counts. Multi-person pose
results are treated as ambiguous. Vision work and GPU initialization use one thread.

Position is a conservative heuristic with required visibility ≥0.65 and
opposite-side arm evidence relative to the torso axis. MediaPipe world Y is not
a ground/gravity sensor. Camera cannot prove supine orientation or firm surface.
Green means a stable estimate, NOT clinical validation. Confirmation bounce
is triggered once per event ID.

The chest marker interpolates 0.45 from shoulder midpoint toward hips; it is
an approximate target. Single-hand visual tracking remains supported, but adult
spoken instructions describe standard two-hand technique. Do not restore patient
pose wrists as rescuer-hand fallback, or indefinitely cached missing targets.

## Compression coaching

The 110 BPM sphere is an independent pacing cue, configurable within 100–120.
It does not count compressions or measure depth.

CompressionDetector counts estimated torso-relative tracked-hand motion cycles
only in active coaching near the target. Excursion, temporal separation,
monotonic timestamps, stable hand identity and tracking-gap handling prevent
simple duplicates/noise from fabricating progress. Rate requires at least four
recent events and consistent intervals. Alerts have hysteresis and cooldown.
Tracking loss invalidates rate/current cycle but preserves the cumulative estimate.

No claims of validated compression depth, pulse, perfusion, anatomical accuracy,
or clinical efficacy. The old simulated-looking vitals panel is not displayed.

## Important functional limitations

- Legacy autonomous stroke/allergy/heart-attack modules remain in source but
  are not dispatched by the new coordinator. Concern cards lead to emergency
  assessment/dispatcher guidance; specialty protocols need review before restoring.
- English phrase grammar, not unrestricted conversational understanding.
- No process-death session persistence; ViewModel handles Activity recreation.
- Camera alignment, posture acceptance and count accuracy need controlled real
  device/manikin validation. Tests use synthetic signals, not clinical data.
- Clinical approval and India-specific protocol review are still outstanding.
- This is an adult guidance prototype. Follow emergency dispatchers/AEDs first.

## Verification and device handoff

Last code verification:
`./gradlew testDebugUnitTest assembleDebug lintDebug` — BUILD SUCCESSFUL.
36 tests passed: workflow/dialogue 16, compression detector 6,
vision/overlay/engine integration 10, voice lifecycle 4.
Lint succeeds with warnings. `git diff --check` passes.

Subsequently installed the debug APK on the user's I2501 phone
(serial `10BFC41SG2001UZ`) using `adb install -r`; result Success.
Launched `com.sanjeevani/.MainActivity`; result Status: ok, cold launch 343 ms.
The app process was present afterward. A short filtered AndroidRuntime log read
returned no error output. This is installation/launch smoke verification ONLY.
Do not claim the live microphone, speaker, camera geometry, or counting has been
verified end-to-end on the phone. The manual checklist is in the report.

## Commands and local assets

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
/Users/naman/Library/Android/sdk/platform-tools/adb devices -l
/Users/naman/Library/Android/sdk/platform-tools/adb -s 10BFC41SG2001UZ install -r app/build/outputs/apk/debug/app-debug.apk
/Users/naman/Library/Android/sdk/platform-tools/adb -s 10BFC41SG2001UZ shell am start -W -n com.sanjeevani/.MainActivity
```

Debug APK is about 577 MB due to local assets. MediaPipe and Kokoro model files
are not committed. Use existing `download_models.sh` / `setup_kokoro.sh` when
setting up a fresh machine; no downloads are needed for this workspace.
SDK location is in untracked local.properties.

Existing untracked `download_llm.sh` and `sanjeevani_system_design.png/.tex`
were left outside this handoff commit. The LLM script's claim that the app loads
a model on startup is outdated; do not run it as part of this workflow.

## Safety and source references

Emergency number: 112 (India). Speak numbers clearly as words.
Clinical transitions must remain deterministic and testable.
Never infer responsiveness/breathing from pose alone, or gate urgent assistance
on getting camera tracking to work. Never claim the implementation is approved.

The report links the 2025 Resuscitation Council UK adult BLS guidance and India
ERSS 112 used during review. Those references do not validate this app.
Older claims here about a completed/verified CPR module and frozen Hanning
algorithm constants were inaccurate for the current implementation.

Landmarks: nose 0; shoulders 11/12; elbows 13/14; wrists 15/16; hips 23/24;
knees 25/26. Hand palm MCP points: 5/9/13/17. Normalized image coordinates
are distinct from world coordinates and handedness labels.

Historical iOS reference:
`/Users/naman/Downloads/iqoo final/Halo/Halo-HackHarvard/SafeStepAR/`.
Its Config.swift contains an exposed ElevenLabs key. Do not read/use/copy that
secret; owner should revoke it. No secret is needed for this implementation.
