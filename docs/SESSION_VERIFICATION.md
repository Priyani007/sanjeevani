# Stateful emergency coach: implementation and verification

Date: 10 October 2026. Branch: feature/person-b-ui.

## Scope and status

Implemented the latest end-to-end debugging brief in the existing Kotlin/Compose/CameraX/MediaPipe application. This is an adult emergency-assistance prototype, not a clinically validated medical device. Clinical review and physical-device testing remain required before any safety or accuracy claim.

The existing working tree already contained extensive changes. They were inspected and incorporated where relevant; unrelated assets/scripts and design files were not discarded. This report and the application changes are included in the user-requested local handoff commit. No remote push was requested.

## Confirmed root causes

| Code path | Finding | Change |
|---|---|---|
| Engine → EmergencyFSM and CPRWorkflow | Two independent progressions; hand/posture cards advanced on timers | EmergencyFSM is the sole authority; CPRWorkflow is a read-only projection |
| Engine cardiac routing | Heart-attack selection enabled CPR cards independently of eligibility | Concern selection leads to responsiveness; unresponsive patients also need a normal-breathing assessment |
| ViewModel opening and responsiveness | Recognition began during speech or after an arbitrary two seconds | Recognition starts from successful speech completion |
| Recognition result handling | Partial transcripts and broad substring matches could mutate the workflow | Final-only, contextual interpretation; low confidence/contradictions clarify |
| Voice companion | Generated sentences could speak asynchronously without state-version validation | Free-form generated medical speech is not loaded or used in the session decision/playback path |
| MediaPipe → PerceptionFrame | Cached inference was assigned the current frame's timestamp | Preserve source timestamps; freshness/synchronization checks; repeated samples do not count |
| SpatialReasoner | Indefinite cached target, low-confidence hips, patient wrists substituted for rescuer hands | Missing/uncertain observations cannot validate; track a visible hand rather than patient pose wrists |
| CompressionDetector | Edge filtering/whole-window prominence and repeated pose-wrist samples lacked strong signal lifecycle | Alternating excursion detector with timestamp, dropout, source-identity, interval and rate-quality checks |
| Overlay | Green used a single accepted frame; animation keyed to text/color | Green is a stable-estimate state with one confirmation event; user still confirms actual positioning |
| MainActivity | 112 button was a placeholder; rotated dimensions did not match output bitmap | Dialer action, dispatcher mute, upright dimensions, FIT_CENTER mapping |

These are source-confirmed findings, not claims that every symptom was reproduced on hardware.

## Authoritative workflow and evidence

| State | Required evidence / permitted next step | Wait or failure behavior |
|---|---|---|
| IDLE / TRIAGE_DETECTION | User reports a concern → RESPONSIVENESS_CHECK | Eight-second recognition window after speech; selection buttons on silence/error |
| RESPONSIVENESS_CHECK | User reports yes → monitoring; no → BREATHING_ASSESSMENT | Never advances because of elapsed time, camera pose, or prompt completion |
| BREATHING_ASSESSMENT | Normal breathing → monitoring; no normal breathing, with reported unresponsiveness → CPR_POSITIONING | Ambiguous answers explain uncertainty and direct to dispatcher; tap alternatives remain |
| CPR_POSITIONING | Fresh, sufficiently visible geometry stable for one second → POSITION_CONFIRMED | No tracking means unconfirmed; explicit user-confirmed positioning can proceed without camera |
| POSITION_CONFIRMED | User confirms actual back position/readiness → HAND_POSITIONING | No animation callback advances the state |
| HAND_POSITIONING | Fresh estimated hand alignment stable for one second, or explicit manual confirmation → POSTURE_CHECK | Screen-relative corrections; missing tracking never means success |
| POSTURE_CHECK | Explicit ready answer and CPR eligibility → COMPRESSION_ACTIVE | No timer-based start |
| COMPRESSION_ACTIVE | Observe motion and provide pacing; explicit changed condition → reassessment | Tracking loss does not reopen positioning; dispatcher always takes priority |
| Monitoring | Explicit changed condition → reassessment | No automatic diagnosis or camera-derived consciousness |
| Any active session | Explicit emergency-team takeover → terminal handoff | No claim that the patient has recovered |

Every transition has a timestamp, reason, source, destination and monotonically increasing version. Completion history is separate from current-step validation. Camera geometry does not set responsiveness or breathing. Structured in-memory context retains the initial report, current question, reported findings, completed states and a bounded recent-answer history. Transcripts are not logged or sent to a server by this implementation.

Non-CPR concerns retain their entry cards, but legacy autonomous stroke/allergy mini-protocols are intentionally not dispatched: those modules contain timer-driven clinical claims, including allergy “stable” claims and stroke face assessment from shoulder asymmetry. Their source remains intact for a separate protocol review. This is a functional limitation, not a completed replacement for those specialty protocols.

## Voice lifecycle and concurrency

The main-thread ViewModel serializes state publication. Speech has a unique turn ID and workflow version. Successful completion may open the recognizer; stale completion, recognition, timeout and audio-focus callbacks cannot act on a newer turn. Tap answers publish immediately, without waiting for a camera frame. Partial recognition results never trigger a safety decision.

Android TTS is primary for lower startup latency, at rate 1.05 and full per-utterance gain, respecting the user's system volume. Kokoro remains a fallback with serialized synthesis, cancellation tokens and playback-head completion rather than estimated-duration completion. Audio focus, dispatcher mute, app pause/resume and failed playback are explicit. Recognition availability, permission/network errors and silence expose honest statuses and tap controls. SpeechRecognizer may require network depending on the installed service; offline speech is not guaranteed.

Vision model creation and inference submission use the same dedicated thread, leaving opening speech/UI initialization independent of model loading. Only one frame is pending through the coordinator. In-flight frames capture state version; source results older than the state transition cannot validate it.

## Position, coordinates and animation

Position acceptance is a conservative screen-space heuristic, not proof of being supine or on a firm surface. It checks visible torso/arms and rejects arms projected on the same side of the body axis. MediaPipe world Y is not treated as a calibrated ground/gravity measurement. A green estimate still asks the user to confirm actual positioning.

Pose visibility threshold is 0.65 for the required torso points; tracked arm evidence must also be sufficiently visible. Source pose age is at most 300 ms, hand age at most 250 ms, and pose/hand timestamp separation at most 150 ms. Multiple detected people are treated as ambiguous rather than silently choosing the first person.

The existing chest target geometry is explicitly approximate. One tracked palm can drive the visual estimate; adult spoken guidance still describes both hands. Directions mean screen left/right/up/down. The back-camera image is rotated upright before inference and is not mirrored. Overlay mapping uses upright image dimensions and FIT_CENTER letterboxing, matching the configured preview. Front-camera support and real-device registration across sensor crops/orientations are not validated.

Confirmation uses a monotonic event ID; repeated frames do not replay the bounce. The compression sphere has an independent 110 BPM configurable pacing cycle, using equal expansion/contraction halves. It remains a labeled pacing cue when tracking is missing and is never presented as depth measurement.

## Counting and pace feedback

Counting uses tracked palm motion relative to the chest target, normalized by torso span, only in COMPRESSION_ACTIVE near the target. It is an estimated motion-cycle count, not a validated compression count. The detector rejects duplicate/out-of-order timestamps, requires excursion and temporal separation, resets its in-progress cycle/rate on dropout or hand-source changes, and preserves cumulative count through tracking loss.

Rate requires at least four recent events with sufficiently consistent intervals and fresh samples. Alerts use the 100–120 BPM range with hysteresis and a four-second cooldown. The pacing animation never increments the count. No depth, perfusion, pulse, or clinical technique validation is claimed. The fake-looking vitals panel is not shown in this session.

## Files changed for this work

- Models.kt: breathing/confirmation states, source timestamps, version and reliability metadata.
- EmergencyFSM.kt, CPRWorkflow.kt: guarded progression, transition history, structured assessment evidence; remove autonomous timer progression.
- SanjeevaniEngine.kt: current-state-only validation, speech priority, freshness checks, derived overlay and session context.
- SessionDialogue.kt: contextual interpretation, deterministic question help, callback turn gate.
- SanjeevaniViewModel.kt: speech/recognition lifecycle, bounded listening, cancellation/version guards, immediate tap results, vision worker.
- MediaPipeController.kt: inference source times, freshness and ambiguous-person handling.
- PatientDetector.kt, SpatialReasoner.kt: conservative confidence gates, body-axis arm checks, no indefinitely cached target or patient-wrist fallback.
- CompressionDetector.kt: motion-cycle signal lifecycle, rate confidence and hysteresis.
- KokoroTTS.kt: completion/error callback, serialized generation, guarded playback cancellation and shutdown.
- MainActivity.kt, EmergencySelectionScreen.kt: 112 dialer, permission/lifecycle handling, tap controls, honest status and concern wording.
- AROverlayView.kt, OverlayCoordinates.kt: event-driven confirmation, independently paced sphere, estimates/uncertainty labels and fitted coordinates.
- app/build.gradle.kts and four test classes: JUnit/Robolectric verification.

## Automated verification

Command: ./gradlew testDebugUnitTest assembleDebug lintDebug

Final run: BUILD SUCCESSFUL (16 seconds). All 36 tests passed, zero failures/errors/skips:
16 workflow/dialogue, 6 motion detector, 10 vision/overlay/engine integration, and 4 voice lifecycle tests.
Debug APK assembled, lint completed successfully with warnings, and git diff --check passed.

The test suite covers contextual answers/questions, clinical entry gates, stale/duplicate events, stable visual holds, completed-step persistence, explicit reassessment, directional geometry, unavailable tracking, source timestamps, animation event deduplication/cadence, speech completion/failure/interruption, immediate camera-free tap transitions, and a controlled end-to-end session.

Synthetic signals at 80, 110 and 140 BPM test counting and pace categories. They are simulated inputs, not measured real-world compression performance. Robolectric exercises Android code on the JVM; it is not a physical microphone, speaker, GPU, or camera test.

Build output: app/build/outputs/apk/debug/app-debug.apk.
JUnit results: app/build/test-results/testDebugUnitTest/.
Human-readable tests: app/build/reports/tests/testDebugUnitTest/index.html.
Lint: app/build/reports/lint-results-debug.html. Lint succeeds with warnings; dependency/deprecation cleanup is outside this fix.

## Device verification and remaining acceptance work

At initial verification, ADB reported device 10BFC41SG2001UZ as unauthorized. After the user authorized it, the debug APK was installed successfully on the I2501 and MainActivity launched with Status: ok (cold launch 343 ms). The app process was present afterward; a short filtered AndroidRuntime log read returned no error output. Installation/launch smoke verification succeeded. Live speech, camera registration, posture recognition and physical compression-count accuracy remain unverified; no end-to-end device-level success is claimed.

Next verify with a training manikin/controlled setup, not practice compressions on a healthy person:

1. Launch, listen to the opening, and verify recognizer starts only after speech ends.
2. Report suspected heart attack; test responsive and unresponsive/normal-breathing branches.
3. Exercise the unresponsive/abnormal-breathing branch using voice and taps; interrupt and repeat prompts.
4. Present same-side arms, occluded landmarks and a clearly positioned manikin; check red/green event and manual fallback.
5. Validate marker registration and all directions under portrait/landscape, camera movement, occlusion and multiple-person scenes.
6. Compare estimated cycles with annotated video at known rates; measure false positives, missed cycles and latency.
7. Disconnect tracking, mute for dispatcher, leave/return to the app, deny permissions and test recognition/network failure.
8. Confirm that no old step or stale audio reappears and that the emergency-team handoff is terminal.

Session state survives Activity recreation through ViewModel, but process-death persistence is not implemented. Language understanding is a conservative English phrase grammar, not unrestricted conversation. Visual position, anatomical target and motion-count thresholds require dataset/device validation. The app has not received clinical approval.

## Guidance sources

The assessment distinction and adult CPR parameters were checked against [Resuscitation Council UK adult basic life support guidelines (2025)](https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/adult-basic-life-support-guidelines). Emergency contact is localized to India's [112 Emergency Response Support System](https://112.gov.in/). These references do not constitute approval of this app, its camera algorithm, or a complete India-specific clinical protocol.
