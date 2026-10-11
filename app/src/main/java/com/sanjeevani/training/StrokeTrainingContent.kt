package com.sanjeevani.training

/**
 * 8-step interactive stroke FAST training content.
 * Based on Stroke Foundation India / ASA FAST guidelines.
 *
 * Safety note: facial appearance alone must not be used to diagnose or rule out stroke.
 * These scenarios train recognition of warning signs and the correct escalation response.
 */
object StrokeTrainingContent {

    private const val MODULE = "STROKE_FAST"

    val steps: List<ModuleTrainingStep> = listOf(

        // ── Step 0: FAST acronym recognition ──────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 0,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.FAST_RECOGNITION,
            skillDescription = "FAST acronym and why it matters",
            stepTitle        = "What is FAST?",
            scenarioContext  = "A colleague asks you what the FAST acronym stands for in stroke first aid.",
            questionText     = "What does FAST stand for?",
            options = listOf(
                ModuleTrainingOption("A", "Face, Arms, Speed, Treatment",
                    "Close, but not quite right. Speed is not the S. FAST stands for Face, Arms, Speech, Time — each representing a key warning sign or action.",
                    isCorrect = false),
                ModuleTrainingOption("B", "Face, Arms, Speech, Time",
                    "Correct. FAST: Face drooping, Arm weakness, Speech difficulty, Time to call 112. These are the four key indicators used to identify a stroke quickly. (Stroke Foundation India / ASA)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Fever, Arm pain, Slurred speech, Treatment",
                    "Incorrect. FAST is a stroke recognition tool, not a general illness checklist. Fever and arm pain are not part of it.",
                    isCorrect = false),
                ModuleTrainingOption("D", "Face, Arms, Stroke, Time",
                    "Incorrect. The 'S' stands for Speech difficulty, not Stroke. Each letter represents an observable warning sign.",
                    isCorrect = false)
            )
        ),

        // ── Step 1: Face drooping ─────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 1,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.FACE_DROOPING,
            skillDescription = "Recognising face drooping (F in FAST)",
            stepTitle        = "Face Drooping",
            scenarioContext  = "You notice a friend smiling, but one side of their mouth appears to droop. They say it feels numb. You ask them to smile broadly.",
            questionText     = "One side of their face droops when they smile. What should you do?",
            cameraRequirement = ModuleCameraRequirement.FACE_REFERENCE_VISIBLE,
            options = listOf(
                ModuleTrainingOption("A", "Reassure them — uneven smiles are common and not a medical emergency.",
                    "Incorrect. Sudden one-sided facial drooping is a recognised warning sign of stroke. Do not reassure and dismiss. Act urgently.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Treat this as a possible stroke — check arm and speech, then call 112 immediately.",
                    "Correct. Sudden facial drooping (F in FAST) is a warning sign of stroke. Check arms and speech, and call 112 without delay. Time-critical brain treatment is available only in hospital. (Stroke Foundation India)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Wait 30 minutes to see if the drooping resolves before seeking help.",
                    "Incorrect. Stroke symptoms that resolve may indicate a transient ischaemic attack (TIA), which is also a medical emergency. Never wait — call 112 immediately.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Give them water and aspirin to prevent further damage.",
                    "Incorrect. Never give food, drink, or medication to someone with possible stroke symptoms — swallowing may be impaired and the stroke type is unknown. Call 112.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 2: Arm weakness ──────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 2,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.ARM_WEAKNESS,
            skillDescription = "Recognising arm weakness (A in FAST)",
            stepTitle        = "Arm Weakness",
            scenarioContext  = "You ask someone to raise both arms in front of them, palms up, and hold for 10 seconds.",
            questionText     = "One arm drifts downward while the other stays up. What does this indicate, and what do you do?",
            cameraRequirement = ModuleCameraRequirement.BOTH_ARMS_VISIBLE,
            options = listOf(
                ModuleTrainingOption("A", "They are probably tired or have a shoulder injury — suggest rest.",
                    "Incorrect. Sudden unilateral arm drift in this test is arm weakness (A in FAST), a warning sign of stroke, not fatigue.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Ask them to try harder — one arm drifting does not mean anything serious.",
                    "Incorrect. One arm drifting involuntarily in the FAST arm check is a positive finding indicating possible stroke. Do not dismiss it.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("C", "This is a positive FAST arm check — treat as possible stroke and call 112 now.",
                    "Correct. Sudden one-sided arm drift is a FAST warning sign. A single new FAST sign is enough to treat this as possible stroke and call 112 immediately.",
                    isCorrect = true),
                ModuleTrainingOption("D", "Support both their arms and massage the weak arm to restore circulation.",
                    "Incorrect. Massaging the arm does not treat stroke and delays emergency escalation. Call 112.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 3: Speech difficulty ─────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 3,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.SPEECH_DIFFICULTY,
            skillDescription = "Recognising speech difficulty (S in FAST)",
            stepTitle        = "Speech Difficulty",
            scenarioContext  = "You ask someone to repeat the sentence 'The sky is blue today.' Their speech is slurred and confused; they cannot complete the sentence.",
            questionText     = "Sudden slurred, confused, or absent speech — what does this indicate?",
            options = listOf(
                ModuleTrainingOption("A", "They may be intoxicated — offer water and monitor them.",
                    "Incorrect. While intoxication can mimic speech difficulty, sudden onset in a previously sober person requires a FAST check. Do not assume and delay assessment.",
                    isCorrect = false),
                ModuleTrainingOption("B", "Speech difficulty (S in FAST) — this is a possible stroke warning sign. Call 112.",
                    "Correct. Sudden slurred, confused, or missing speech is the S in FAST and a key stroke warning sign. Call 112 immediately. Note the time symptoms started — this is critical information for hospital teams. (Stroke Foundation India / AHA)",
                    isCorrect = true),
                ModuleTrainingOption("C", "This indicates a panic attack — help them breathe slowly.",
                    "Incorrect. Panic attacks cause breathing difficulty, not sudden slurred speech. Do not delay calling 112.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Wait for the symptom to pass — sudden speech problems often resolve.",
                    "Incorrect. Even if symptoms resolve, this could be a TIA (mini-stroke), which carries high short-term stroke risk. Always call 112 for new sudden neurological symptoms.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 4: Time to act ───────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 4,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.TIME_TO_ACT,
            skillDescription = "Time is critical — call 112 immediately (T in FAST)",
            stepTitle        = "Time to Call 112",
            scenarioContext  = "Someone shows one or more positive FAST signs. It is 9 pm and the person does not want to go to hospital.",
            questionText     = "The person refuses help, saying the symptoms will pass. What do you do?",
            options = listOf(
                ModuleTrainingOption("A", "Respect their choice and monitor them at home.",
                    "Incorrect. Stroke is a medical emergency. If someone with stroke symptoms refuses help, strongly encourage them; call 112 and let emergency services advise. Time-critical treatments are only available in hospital.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Call 112 immediately regardless, note the time symptoms started, and stay with them.",
                    "Correct. Stroke treatment — particularly clot-dissolving or clot-removing therapy — has a narrow time window. Call 112 now. Note the exact time symptoms were first noticed. Stay with the person and keep them calm. (T in FAST — Time is brain.) (Stroke Foundation India)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Drive them to hospital yourself to save time.",
                    "Partially right that hospital is needed, but calling 112 is better — paramedics can begin assessment and pre-notify the hospital, reducing time to treatment. Driving may delay care.",
                    isCorrect = false),
                ModuleTrainingOption("D", "Give them aspirin, which helps prevent further clotting.",
                    "Incorrect. Aspirin is not appropriate pre-diagnosis — haemorrhagic (bleeding) stroke would be worsened by aspirin. Never give medication. Call 112.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 5: Positioning ───────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 5,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.SAFE_POSITIONING,
            skillDescription = "Safe positioning while waiting for emergency services",
            stepTitle        = "Safe Positioning",
            scenarioContext  = "You have called 112. The person is conscious and has one-sided weakness. Emergency services are 10 minutes away.",
            questionText     = "How should you position the person while waiting?",
            options = listOf(
                ModuleTrainingOption("A", "Help them stand and walk to stay active and maintain circulation.",
                    "Incorrect. Moving someone with stroke symptoms increases fall risk and may worsen their condition. Keep them still and comfortable.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Keep them still in a comfortable supported position; if unconscious but breathing, use the recovery position and follow 112 instructions.",
                    "Correct. Avoid unnecessary movement, keep them comfortable, monitor breathing, and follow the 112 dispatcher's positioning instructions. Use the recovery position if unconscious but breathing.",
                    isCorrect = true),
                ModuleTrainingOption("C", "Lay them flat on their back to improve blood flow to the brain.",
                    "Not the best general rule. Do not force a fixed position to 'improve blood flow'; keep them comfortable and follow the 112 dispatcher's instructions.",
                    isCorrect = false),
                ModuleTrainingOption("D", "Have them lie on the affected side to protect the weak arm.",
                    "Incorrect. Positioning should be guided by consciousness and comfort, not by trying to protect the weak side. Follow dispatcher instructions.",
                    isCorrect = false)
            )
        ),

        // ── Step 6: Do not give food or drink ────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 6,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.DO_NOT_GIVE_FOOD,
            skillDescription = "Do not give food, drink or medication during stroke",
            stepTitle        = "Food and Medication",
            scenarioContext  = "While waiting for ambulance, the person says they are thirsty and asks for water. Another bystander suggests giving them aspirin.",
            questionText     = "Should you give the person water or aspirin?",
            options = listOf(
                ModuleTrainingOption("A", "Give water to prevent dehydration, but not aspirin.",
                    "Incorrect. Stroke impairs swallowing (dysphagia) even when the person does not realise it. Water or food risk aspiration pneumonia. Do not give anything by mouth.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Do not give food, drink or medication — swallowing may be impaired, and aspirin is contraindicated until stroke type is confirmed.",
                    "Correct. Stroke commonly impairs swallowing reflexes; anything given by mouth may enter the airway. Aspirin may worsen a haemorrhagic stroke. Only hospital assessment can determine stroke type. (Stroke Foundation India / NICE Guidelines)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Give both water and aspirin — aspirin is standard for all strokes.",
                    "Incorrect. Aspirin is harmful in haemorrhagic stroke. Stroke type must be established in hospital before aspirin is given. Do not administer anything.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Give them juice if they are conscious — only solids are dangerous.",
                    "Incorrect. Liquids also carry aspiration risk in stroke with dysphagia. Nothing by mouth until swallowing is assessed by a clinician.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 7: TIA recognition ───────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 7,
            moduleType       = MODULE,
            skillTag         = StrokeSkillTag.TIA_RECOGNITION,
            skillDescription = "Transient ischaemic attack (TIA) — still a medical emergency",
            stepTitle        = "Mini-Stroke (TIA)",
            scenarioContext  = "A family member had slurred speech and arm weakness for about 5 minutes, but now they feel completely normal. They say they are fine and want to go to bed.",
            questionText     = "The symptoms have resolved. Is this still an emergency?",
            options = listOf(
                ModuleTrainingOption("A", "No — if symptoms resolve completely, no further action is needed.",
                    "Incorrect. Resolving symptoms may indicate a TIA (transient ischaemic attack), also called a mini-stroke, which carries a high risk of full stroke in the following hours and days.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Yes — resolved stroke-like symptoms may be a TIA. Seek emergency assessment immediately.",
                    "Correct. TIA symptoms resolve within 24 hours (often within minutes) but still require urgent medical assessment. Up to 20% of TIA patients have a full stroke within 90 days, with the highest risk in the first 48 hours. Call 112 or go to emergency immediately. (NICE TIA guidelines / Stroke Foundation India)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Only call if symptoms return — resolved symptoms mean the episode was minor.",
                    "Incorrect. The resolution of symptoms does not reduce urgency. TIA is a medical warning and a missed opportunity for preventive treatment. Seek assessment now.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Give them aspirin and monitor overnight.",
                    "Incorrect. Aspirin should only be given after imaging confirms an ischaemic event. Self-medication at home delays the diagnostic assessment needed to prevent a full stroke.",
                    isCorrect = false, isSafe = false)
            )
        )
    )

    fun stepFor(stepIndex: Int): ModuleTrainingStep? = steps.getOrNull(stepIndex)

    fun allSkillTags(): List<String> = steps.map { it.skillTag }.distinct()
}
