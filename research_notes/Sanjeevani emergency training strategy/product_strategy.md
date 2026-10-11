# Sanjeevani Product Strategy — iQOO Hackathon 2026

Research date: October 10, 2026. Grand Finale is running now (Bengaluru, Oct 9–11, 2026).

---

## 1. Judging Rubric (Confirmed)

Source: [iQOO Hackathon 2026 official blog via Reskilll](https://blogs.reskilll.com/iqoo-hackathon-2026-india-phone-first-ai-hackathon-iqoo-reskilll/)

| Criterion | Weight | Measured by |
|---|---|---|
| End product quality | 30% | Jury |
| Novelty and impact | 20% | Jury |
| Creative phone use (camera, voice, on-device AI) | 15% | HackTracker device data |
| Technical depth | 15% | Jury |
| HackTracker Office Kit usage | 10% | HackTracker device data |
| Demo and presentation | 10% | Jury |

**Critical insight:** HackTracker is an automatic device telemetry recorder. It captures actual phone usage, not what the team claims. 25% of the total score (phone use + Office Kit) is determined by this data alone — it cannot be faked in the pitch.

---

## 2. HackTracker Office Kit — What It Is

Source: [iQOO Hackathon 2026 blog](https://blogs.reskilll.com/iqoo-hackathon-2026-india-phone-first-ai-hackathon-iqoo-reskilll/); [Strategy guide](https://blogs.reskilll.com/how-to-win-iqoo-city-battles-strategy-guide-phone-first-ai-hackathon/)

Office Kit is the iQOO phone-to-laptop bridge tool (download: pc.vivoglobal.com). Features:
- **Screen mirroring** — show phone screen on laptop
- **Shared clipboard** — copy/paste between phone and laptop
- **File transfer** — move files wirelessly
- **Remote control** — control phone from laptop keyboard/mouse

HackTracker logs Office Kit usage and reports it as 10% of the score. The team must actively use it during the build, not just mention it.

**Integration opportunity for Sanjeevani:**
- Use screen mirroring to display Sanjeevani's training session on a laptop (facilitator view / demo mode)
- Use file transfer to export session logs or training progress reports to the laptop
- Use remote control during demo to navigate modules from laptop while the phone stays in a "patient simulation" view
- Keep Office Kit running throughout the build to log usage time

---

## 3. India Bystander CPR Context (Verified Statistics)

Sources: [ORF commentary](https://www.orfonline.org/index.php/expert-speak/prioritising-first-aid-cpr-and-bls-training-for-a-healthier-india); [F1000Research](https://f1000research.com/articles/11-469)

- India bystander CPR rate: **1.3% to 9.8%** of out-of-hospital cardiac arrests
- **Nearly 90% of OHCAs in India go without any CPR**
- USA bystander CPR rate: 45.7%; Sweden: 68.2%
- In a survey of 583 urban Indian adults aged 18–35, only 101 had received CPR training; only 28% had heard of AED
- In a survey of 1,851 students, only 29.17% had prior CPR training; only 57% were willing to initiate CPR

**Pitch use:** These are verified, citable statistics that prove the problem is real and large. The gap between India (under 10%) and comparable high-income countries (45–68%) is the core impact claim. Do not invent survival rate improvements — cite only the documented CPR rate disparity.

---

## 4. CPR Training App Research

Source: [JMIR 2014 review](https://jmir.org/2014/3/e89); [NCBI Stockholm CPR alert study](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3978555/); [MobiHealthNews app quality review](https://mobihealthnews.com/news/study-most-cpr-apps-are-hard-use-dont-meet-guidelines)

- A 2026 Chinese nonrandomized trial found that a game-based CPR training app improved short-term knowledge/skills and 6-month retention as a complement to conventional training
- A 2014 review found that very few CPR apps are designed per current BLS guidelines or offer acceptable usability — most apps in app stores fail basic quality tests
- Stockholm CPR alert study: when CPR-trained bystanders were notified by phone of nearby cardiac arrests, bystander CPR rate rose from 48% to 62%

**Implication for Sanjeevani:** The evidence supports apps as *practice supplements* between instructor-led sessions, not as standalone replacements. This is differentiator hypothesis B and is the most evidence-backed framing.

---

## 5. On-Device AI / Camera Coaching Technical Feasibility

Sources: [Arm learning path — AI plank tutor](https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/ai-plank-tutor/7-extend/); [SnapOn hackathon project](https://lablab.ai/ai-hackathons/qualcomm-x-meta-executorch-hackathon/snapon); [LiteRT Snapdragon accelerator](https://www.infoq.com/news/2025/11/litert-snapdragon-accelerator)

A working camera-based posture coaching pipeline for Android exists:
1. CameraX captures live frames
2. ML Kit or MediaPipe detects pose landmarks
3. Landmarks compared against reference posture
4. Local LLM (Phi-3, Gemma 2B, or Whisper Tiny) generates feedback
5. Android TTS speaks the feedback

Qualcomm's open-source AI Hub Apps repo covers: LLMs (GenAI), image classification, object detection, and speech-to-text. All run on the Snapdragon Hexagon NPU.

Google's LiteRT QNN path: benchmarked across 72 ML models, 64 achieved full NPU delegation.

**For CPR coaching specifically:** the camera can detect whether the learner's hands are correctly positioned and whether they are pressing at roughly the right rate. Real-time audio (microphone) can detect compression rhythm via sound. This is feasible on-device without cloud inference.

---

## 6. Ranked Scoring of the 5 Differentiator Hypotheses

### Hypothesis A: Closed-Loop Skill Coaching (observe error → feedback → recheck)
**Score: 9/10**

Evidence of user need:
- 90% of Indian OHCAs receive no CPR. Low skill confidence is a primary barrier.
- 2026 Chinese study: app + real-time feedback improved skill acquisition and retention vs. text-only
- Arm plank tutor demonstrates the technical pipeline is buildable in a hackathon timeframe

Feasibility: HIGH. MediaPipe pose detection + local LLM + TTS is a proven stack. Works on Snapdragon NPU.

Demonstrability: VERY HIGH. Live demo: judges see the phone camera watching someone simulate CPR compressions, then watch the app give spoken feedback in real time. This is a 30-second "wow moment."

HackTracker alignment: Camera (15%) + on-device AI (part of technical depth 15%) = direct score contribution.

**Verdict: Strongest differentiator. Implement as primary feature.**

---

### Hypothesis B: Practice Supplement Between Instructor Sessions
**Score: 7/10**

Evidence: Strong. 2026 Chinese study is direct evidence for this framing. App-as-complement is the safest clinical claim.

Feasibility: HIGH. This is what the app already is. No additional building required.

Demonstrability: MEDIUM. Hard to show "between sessions" in a 3-minute demo. It's a positioning claim, not a feature.

**Verdict: Use as pitch narrative framing, not as a standalone differentiator feature.**

---

### Hypothesis C: Multi-Skill Training Engine (configurable modules)
**Score: 6/10**

Evidence: Moderate. Multiple emergency skills (CPR, allergic reaction, stroke FAST) in one app has user value for breadth. But breadth alone is not a competitive moat.

Feasibility: HIGH. Already implemented.

Demonstrability: MEDIUM. Can demo 3 modules but risks looking shallow unless each is polished.

**Verdict: Use as supporting proof of platform vision, not primary differentiator.**

---

### Hypothesis D: Adaptive Scenario Branching
**Score: 5/10**

Evidence: Limited specific research found on adaptive branching vs. fixed scenarios for emergency training. General adaptive learning research supports it but is not emergency-specific.

Feasibility: MEDIUM. Requires significant additional build time. Risk of partially finished feature.

Demonstrability: LOW. Hard to show branching logic in 3 minutes.

**Verdict: Mention as roadmap item only. Do not attempt to build in hackathon timeframe.**

---

### Hypothesis E: Objective Progress Tracking Across Sessions
**Score: 5/10**

Evidence: Moderate. Skill retention research supports periodic retesting. But pure tracking without coaching feedback is not differentiated from a simple quiz app.

Feasibility: HIGH. Standard database + charts.

Demonstrability: LOW-MEDIUM. Dashboard screenshots are not a "wow moment."

**Verdict: Implement as lightweight supporting feature (session history with score). Not primary differentiator.**

---

## 7. MVP Recommendation

**Recommendation: Option B — One Deeply Polished Flagship, with the CPR module as flagship.**

Rationale:
- iQOO rubric weights End Product Quality at 30%. "A well-built simple product beats a broken complex one" — exact language from the official Reskilll prep guide.
- A live real-time camera feedback demo during CPR is a decisive "wow moment" that multi-module breadth cannot match.
- The other two modules (allergic reaction, stroke FAST) serve as supporting evidence of platform depth — show them briefly, do not deep-demo them.

**Exact MVP Feature List:**

**Must-have (demonstrate live):**
1. CPR module with real-time camera-based compression guidance
   - Camera detects hand position (acceptable/not acceptable)
   - Microphone detects compression rhythm
   - On-device AI (MediaPipe + local LLM or rule engine) generates coaching feedback
   - Android TTS speaks feedback in real time ("Good rhythm — press harder")
2. Office Kit integration: screen mirror CPR session to laptop for facilitator/judge view
3. Post-session score: compression rate accuracy %, hand position accuracy %

**Should-have (functional, show briefly):**
4. Allergic reaction module (existing — polish UI)
5. Stroke FAST module (existing — polish UI)
6. Emergency call shortcut (112) on every module end screen

**Nice-to-have (show in screenshots or slides only if time permits):**
7. Session history / progress tracking (simple list of past sessions with scores)

**Explicitly out of scope:**
- Adaptive branching (too much build time, low demo payoff)
- Backend dashboard or cloud sync (adds complexity without HackTracker credit)
- Module configurability UI (not needed for demo)

---

## 8. Hackathon Rubric Optimization Map

| Criterion | Weight | What to Build / Demo |
|---|---|---|
| End product quality (30%) | PRIMARY | Polish the CPR module to production quality. Working camera feedback, smooth animations, no crashes. Demo this for 90 seconds live. |
| Novelty and impact (20%) | HIGH | Lead pitch with India CPR stats (90% gap). Real-time camera AI coaching for emergency skills is genuinely novel — no major app store competitor does live pose-based CPR feedback on-device. |
| Creative phone use (15%) | MUST-DO | Camera (compression detection) + Microphone (rhythm detection) + On-device AI (Snapdragon NPU, Phi-3 or MediaPipe) + TTS (spoken feedback). ALL of these log to HackTracker. |
| Technical depth (15%) | HIGH | Explain the pipeline: CameraX → MediaPipe pose landmarks → compression rate algorithm → on-device LLM prompt → TTS. Mention NPU acceleration. Show inference time (target: under 200ms latency). |
| Office Kit usage (10%) | MUST-DO | Screen mirror the CPR demo to laptop during the presentation. Use file transfer to share a session log file. Keep Office Kit connected throughout the build. |
| Demo and presentation (10%) | HIGH | 3-minute live demo script (see Section 10). Open with problem stats, then live demo, then multi-module proof, then pitch future. |

---

## 9. Phone Capabilities — Creative Use Plan

**Camera:** Use front or rear camera to watch learner performing CPR compressions. MediaPipe Pose or a custom gesture model detects hand landmarks relative to chest. No specialist equipment needed — the phone rests nearby or is propped up.

**Microphone:** Record compression sounds to count rhythm. A simple peak detection algorithm on audio amplitude can count compressions/minute without any ML. This is novel, measurable, and entirely on-device.

**On-device AI (Snapdragon NPU):** Run Phi-3 Mini or Gemma 2B for natural language feedback generation. Alternatively, use a rule engine for feedback and Whisper Tiny for speech recognition to accept voice commands ("start", "stop", "replay"). The NPU use is the differentiator vs. cloud-API competitors.

**Text-to-Speech (TTS):** Android TTS (free, on-device) narrates feedback. Gives "coach voice" feel during practice.

**Haptics:** Use vibration to cue compression rhythm — vibrate at 100–120 BPM to help learner stay on pace. No ML required, high UX value.

**Competitors almost certainly not using:** camera-based compression detection + microphone rhythm counting + NPU inference + haptic rhythm guide in an emergency training context.

---

## 10. Timed 3-Minute Demo Script

**0:00–0:25 — The Problem (25 seconds)**
"Every 2 minutes, someone in India suffers a cardiac arrest outside a hospital. Less than 10% receive any CPR before the ambulance arrives — compared to 68% in Sweden. The gap is not willingness. It's practice. Most people trained in CPR forget the skills within 3 months because there's no way to practice between classes. Sanjeevani fixes that."

**0:25–1:45 — Live Demo (80 seconds)**
Open the CPR module on the iQOO. Show the phone propped up facing the practitioner (or a judge volunteer, if possible).
- "I tap Start. The camera activates. I'll simulate a chest compression."
- Perform 5–6 compressions. Show the app detecting hand position and giving real-time spoken feedback ("Good position — maintain your rate").
- "The microphone counts my compression rhythm — 95 per minute. Target is 100 to 120."
- Show the post-session score card: compression rate accuracy, hand position accuracy.
- "This runs entirely on the phone. No cloud, no specialist equipment. Just the iQOO camera and microphone."

**1:45–2:10 — Platform Depth (25 seconds)**
"CPR is one of three modules. We also cover allergic reaction management and stroke recognition using the FAST test."
- Tap into the Allergic Reaction module, show 5 seconds of the scenario.
- Tap into Stroke FAST, show 5 seconds.
- "Three of the five leading preventable death causes in India — covered in one app."

**2:10–2:45 — Technical and Phone-First (35 seconds)**
"The AI runs on the Snapdragon NPU — under 200 milliseconds per inference, fully offline. We use Office Kit to mirror this session to the facilitator's laptop, so a trainer can supervise 10 students simultaneously."
- Show screen mirror on laptop via Office Kit.

**2:45–3:00 — Close (15 seconds)**
"Sanjeevani doesn't replace first aid training. It makes the skills stick between classes. With 90% of Indian cardiac arrest patients receiving no CPR from bystanders, every percent of skill retention that improves doubles the chance someone survives. Sanjeevani."

---

## 11. Three Pitch Versions

**One sentence:**
"Sanjeevani is an Android app that uses your phone's camera and microphone to coach you through CPR practice in real time — so the skill you learned in class is still there when it matters."

**30 seconds:**
"In India, fewer than 10% of cardiac arrest patients receive CPR before the ambulance arrives. The skills exist — people are trained — but skill decay sets in within months, and there's nowhere to practice. Sanjeevani uses the phone camera and microphone to give you real-time coaching feedback during CPR practice drills. It runs entirely on-device, no specialist equipment needed. Three modules: CPR, allergic reaction, and stroke recognition. Practice before the emergency."

**2 minutes:**
"Out-of-hospital cardiac arrest kills hundreds of thousands of Indians every year. The bystander CPR rate in India is under 10%. In Sweden, it is 68%. That gap is not about awareness or willingness — surveys show most people are willing to help. The gap is confidence. People who get trained forget the skills within months because they have no way to practice.

Sanjeevani is a phone-first emergency skills training companion. You open the CPR module, prop your phone nearby, and it uses the camera and microphone to watch you practice. It detects your hand position relative to the chest. It counts your compression rate from sound. An on-device AI model — running on the Snapdragon NPU, no cloud needed — gives you spoken coaching: 'Good position. Speed up your compressions. Target is 100 per minute.'

After every session you see your score: compression rate accuracy, hand positioning accuracy. Practice improves both. The session logs, so you can track improvement over time.

We also cover allergic reaction management and stroke recognition using the FAST test — three of the five most common preventable emergency scenarios.

Everything runs on the iQOO phone, on-device, works offline. No doctor required to use it. No AED, no mannequin. Just you and your phone, building a skill that could save a life.

Sanjeevani — emergency skills that stay with you."

---

## 12. Top 3 Risks to Avoid

### Risk 1: Camera feedback demo fails live
**Why it's dangerous:** 30% of judging is end product quality. A broken demo of the primary differentiator destroys the score.
**Mitigation:**
- Have a pre-recorded fallback video of the camera feedback working — show it if the live demo fails
- Test on the exact iQOO device at least 10 times before the presentation
- Prepare a simplified rule-engine version as backup (if the ML model doesn't load, fall back to timed compression counting only)

### Risk 2: Over-claiming medical efficacy
**Why it's dangerous:** Health judges will ask "where is the clinical evidence?" If you claim Sanjeevani will improve survival rates, a technically literate judge will destroy the claim. Medical claims without citations are disqualifying at health-tech hackathons.
**Mitigation:**
- Claim only what is cited: "CPR training with feedback improves short-term skill acquisition" (cite the 2026 Chinese study)
- Never claim a specific survival rate improvement
- Position clearly as a practice supplement, not a medical device or replacement for instructor training
- Have the ORF/F1000Research India CPR statistics ready to cite when challenged on impact

### Risk 3: HackTracker shows low phone usage
**Why it's dangerous:** 25% of the score comes from device telemetry. If camera/microphone features are implemented but spend most of the build as stub code while real development happens on a laptop, HackTracker will show low phone engagement and you lose the score you need.
**Mitigation:**
- Keep Office Kit running throughout the entire build
- Test the camera feedback feature on the iQOO early and often, not just at demo time
- Use the phone to record debugging sessions and UI tests so camera/mic usage logs
- Assign one team member to "phone-first tasks": UI polish, accessibility testing, sensor integration — work done on-device

---

## 13. Sources

- iQOO Hackathon 2026 judging rubric and HackTracker: https://blogs.reskilll.com/iqoo-hackathon-2026-india-phone-first-ai-hackathon-iqoo-reskilll/
- iQOO City Battle strategy guide: https://blogs.reskilll.com/how-to-win-iqoo-city-battles-strategy-guide-phone-first-ai-hackathon/
- India bystander CPR rate (ORF): https://www.orfonline.org/index.php/expert-speak/prioritising-first-aid-cpr-and-bls-training-for-a-healthier-india
- India vs USA vs Sweden CPR rate comparison: https://f1000research.com/articles/11-469
- 2026 Chinese CPR app RCT (game-based app improves skill): https://mhealth.jmir.org/2026/1/e78689/
- JMIR 2014 CPR app quality review: https://jmir.org/2014/3/e89
- Stockholm CPR bystander alert study (NCBI): https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3978555/
- Arm AI plank tutor (camera-based coaching pipeline): https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/ai-plank-tutor/7-extend/
- SnapOn hackathon project (on-device camera + Whisper + NPU): https://lablab.ai/ai-hackathons/qualcomm-x-meta-executorch-hackathon/snapon
- LiteRT Snapdragon NPU acceleration: https://www.infoq.com/news/2025/11/litert-snapdragon-accelerator
- Hackathon common mistakes guide: https://whereuelevate.com/blogs/why-hackathon-projects-fail
- Health hackathon judging criteria (Mount Sinai, Eventflare): https://icahn.mssm.edu/files/ISMMS/Assets/About the School/Sinainnovations/Hackathon/health_hackathon_scoring.pdf

---

## 14. Key Gaps (Information Not Found)

- No official iQOO or Reskilll documentation on exactly which sensor signals HackTracker records (camera time, mic time, accelerometer, etc.). All sources are Reskilll promotional blog posts. Treat camera/mic/NPU as the safest targets based on available guidance.
- No India-specific first aid training market size figure was found from a primary source. Do not cite a market size in the pitch unless you find a verified figure.
- No competitive analysis was found of existing Android CPR training apps with camera feedback capability. This gap is itself a differentiator signal — if no major competitor does it, the novelty claim is stronger.
- No official HackTracker scoring formula breakdown (e.g., does it weight camera time per minute, or number of camera API calls?). The safest interpretation is: use camera, mic, and on-device AI as the core product features, not as cosmetic additions.
