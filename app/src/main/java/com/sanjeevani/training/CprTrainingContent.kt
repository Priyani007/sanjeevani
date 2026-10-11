package com.sanjeevani.training

/**
 * Static structured training content for CPR mode.
 * Each step maps to a skill tag and contains 4 response options (1 correct, 3 misconceptions).
 * Based on RCUK BLS 2021 / ERC Guidelines 2021.
 */

data class CprTrainingOption(
    val id: String,           // "A", "B", "C", "D"
    val text: String,
    val feedback: String,     // shown after selection — correct or incorrect
    val isCorrect: Boolean,
    val isSafe: Boolean = true // false = dangerous misconception
)

data class CprTrainingStep(
    val stepIndex: Int,
    val skillTag: String,
    val skillDescription: String,
    val stepTitle: String,
    val scenarioContext: String,
    val questionText: String,
    val options: List<CprTrainingOption>,
    val useCameraForAssessment: Boolean = false,
    val cameraAssessmentInstruction: String = ""
) {
    val correctOption: CprTrainingOption get() = options.first { it.isCorrect }
}

object CprTrainingContent {

    val steps: List<CprTrainingStep> = listOf(

        // ── Step 0: Scene Safety & Initial Assessment ──────────────────────────
        CprTrainingStep(
            stepIndex   = 0,
            skillTag    = CprSkillTag.SCENE_SAFETY,
            skillDescription = "Scene safety and initial assessment",
            stepTitle   = "Scene Safety",
            scenarioContext = "You are walking past a park when you see a person lying motionless on the ground. No one else is nearby.",
            questionText = "What is your FIRST action?",
            options = listOf(
                CprTrainingOption("A", "Begin chest compressions immediately.",
                    "Incorrect. Starting compressions without checking for danger could put you at risk, and the person may not need CPR. Always assess the scene first.",
                    isCorrect = false, isSafe = false),
                CprTrainingOption("B", "Shout for help loudly and call 112 before approaching.",
                    "Partially right — calling 112 is essential, but you should also check for danger and assess the person first.",
                    isCorrect = false),
                CprTrainingOption("C", "Check the scene is safe, then approach and check for responsiveness.",
                    "Correct. Ensure the scene is safe before approaching. Then check responsiveness by calling out (\"Are you alright?\") and tapping shoulders firmly. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("D", "Wait 2 minutes to see if the person wakes up on their own.",
                    "Incorrect. Delaying assessment in cardiac arrest can be fatal. Early recognition and action saves lives.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 1: Recognising Cardiac Arrest ────────────────────────────────
        CprTrainingStep(
            stepIndex   = 1,
            skillTag    = CprSkillTag.ARREST_RECOGNITION,
            skillDescription = "Recognition of cardiac arrest",
            stepTitle   = "Recognise Cardiac Arrest",
            scenarioContext = "You checked responsiveness. The person does not respond to your voice or shoulder tap. You look at their chest for up to 10 seconds and see only occasional gasping breaths.",
            questionText = "What does this presentation indicate, and what should you do?",
            options = listOf(
                CprTrainingOption("A", "They have fainted. Place them in the recovery position and monitor.",
                    "Incorrect. A fainting episode produces responsiveness quickly; persistent unresponsiveness with absent or agonal breathing indicates cardiac arrest.",
                    isCorrect = false, isSafe = false),
                CprTrainingOption("B", "They are having a seizure. Do not touch them; clear the area.",
                    "Incorrect. Seizures involve rhythmic movement. Unresponsiveness with absent or agonal breathing (occasional gasps) is cardiac arrest. Gasping is NOT normal breathing.",
                    isCorrect = false),
                CprTrainingOption("C", "This is cardiac arrest. Call 112, then start CPR.",
                    "Correct. Unresponsiveness plus absent or agonal breathing is cardiac arrest. Occasional gasps are not normal breathing. Activate emergency services and start CPR immediately. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("D", "They may be unconscious but breathing. Wait 5 minutes before acting.",
                    "Incorrect. Without a normal breathing check confirmed positive, you must treat this as cardiac arrest. Every minute without CPR significantly reduces survival.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 2: Emergency Escalation ──────────────────────────────────────
        CprTrainingStep(
            stepIndex   = 2,
            skillTag    = CprSkillTag.EMERGENCY_ESCALATION,
            skillDescription = "Emergency escalation — calling 112",
            stepTitle   = "Call Emergency Services",
            scenarioContext = "You have recognised cardiac arrest. You are the only bystander present.",
            questionText = "What is the correct approach to emergency escalation?",
            options = listOf(
                CprTrainingOption("A", "Complete 2 minutes of CPR first, then call 112.",
                    "Incorrect. Activating emergency services must happen as early as possible. If alone, call 112 briefly (on speaker) before or immediately after starting compressions.",
                    isCorrect = false, isSafe = false),
                CprTrainingOption("B", "Call 112 immediately (or put on speakerphone), then start CPR without delay.",
                    "Correct. Call 112 immediately — the dispatcher can guide you. If alone, put the phone on speaker and start CPR. If bystanders are present, send one person to call while you start CPR. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("C", "Find someone with medical training before calling emergency services.",
                    "Incorrect. Do not delay activating emergency services to search for a healthcare professional.",
                    isCorrect = false, isSafe = false),
                CprTrainingOption("D", "Only call 112 if CPR fails after 5 minutes.",
                    "Incorrect. Calling emergency services must not be delayed. Professional help and defibrillation are required urgently.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 3: Patient Positioning ───────────────────────────────────────
        CprTrainingStep(
            stepIndex   = 3,
            skillTag    = CprSkillTag.PATIENT_POSITIONING,
            skillDescription = "Patient positioning for CPR",
            stepTitle   = "Position the Patient",
            scenarioContext = "Emergency services are alerted. You are preparing to start CPR. The person is lying on a soft carpet.",
            questionText = "How should the patient be positioned for effective chest compressions?",
            options = listOf(
                CprTrainingOption("A", "In the recovery position (on their side) to protect the airway.",
                    "Incorrect. The recovery position is for breathing, unconscious casualties. CPR requires the patient to be flat on their back so compressions can create blood flow.",
                    isCorrect = false),
                CprTrainingOption("B", "Sitting upright against a wall for stability.",
                    "Incorrect. Upright positioning prevents effective compression of the chest. The patient must be flat.",
                    isCorrect = false, isSafe = false),
                CprTrainingOption("C", "Flat on their back (supine) on the firmest available surface.",
                    "Correct. The patient must be supine on a firm surface. A soft mattress or carpet absorbs compression force. Move the patient to a hard floor if possible. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("D", "On their stomach, as this opens the airway automatically.",
                    "Incorrect. Compressions are delivered to the front of the chest; a prone position makes CPR impossible.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 4: Hand Placement Region ─────────────────────────────────────
        CprTrainingStep(
            stepIndex   = 4,
            skillTag    = CprSkillTag.HAND_PLACEMENT_REGION,
            skillDescription = "Hand placement region for chest compressions",
            stepTitle   = "Hand Placement",
            scenarioContext = "The patient is supine on a hard surface. You are kneeling beside them, ready to begin compressions.",
            questionText = "Where do you place your hands for chest compressions?",
            options = listOf(
                CprTrainingOption("A", "On the left side of the chest, directly over the heart.",
                    "Incorrect. The heart lies centrally in the chest, not only on the left. Compressions target the centre of the breastbone.",
                    isCorrect = false),
                CprTrainingOption("B", "On the centre of the chest on the lower half of the breastbone (sternum).",
                    "Correct. Place the heel of one hand on the lower half of the sternum (centre of the chest). Place the other hand on top and interlock fingers. Fingers must be lifted off the chest. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("C", "High on the chest, just below the collarbone (clavicle).",
                    "Incorrect. Compressions too high on the sternum are less effective. Target the lower half of the breastbone.",
                    isCorrect = false),
                CprTrainingOption("D", "On the abdomen below the ribcage (sub-xiphoid).",
                    "Incorrect. Compressions below the ribcage target the abdomen and risk liver injury. They do not produce cardiac output.",
                    isCorrect = false, isSafe = false)
            ),
            useCameraForAssessment       = true,
            cameraAssessmentInstruction  = "Now demonstrate the hand placement using the camera. Position your hands as if performing CPR on a manikin, then hold still for 3 seconds."
        ),

        // ── Step 5: Body Position & Arm Technique ─────────────────────────────
        CprTrainingStep(
            stepIndex   = 5,
            skillTag    = CprSkillTag.BODY_POSITION,
            skillDescription = "Rescuer body position and arm technique",
            stepTitle   = "Rescuer Posture",
            scenarioContext = "You have placed your hands correctly. You are about to deliver compressions.",
            questionText = "How should you position your arms and body for effective compressions?",
            options = listOf(
                CprTrainingOption("A", "Kneel beside the patient with bent elbows, pushing mostly with your wrists.",
                    "Incorrect. Bent elbows lose power and tire the rescuer quickly. Arms must be straight to use body weight.",
                    isCorrect = false),
                CprTrainingOption("B", "Kneel astride the patient's hips, leaning forward over their chest.",
                    "Incorrect. Kneeling astride risks injury and reduces compression effectiveness. Kneel beside the patient.",
                    isCorrect = false),
                CprTrainingOption("C", "Kneel beside the patient, arms straight, shoulders directly over your hands, using body weight.",
                    "Correct. Lock your elbows, position shoulders directly over your hands, and use body weight (not just arm strength) to deliver compressions. This maximises force while reducing rescuer fatigue. (RCUK BLS 2021)",
                    isCorrect = true),
                CprTrainingOption("D", "Stand and lean over the patient, pushing downward with fingertips.",
                    "Incorrect. Standing reduces force and using fingertips is ineffective. Use the heel of your hand with straight arms.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 6: Compression Rate ──────────────────────────────────────────
        CprTrainingStep(
            stepIndex   = 6,
            skillTag    = CprSkillTag.COMPRESSION_RATE,
            skillDescription = "Compression rate 100–120 per minute",
            stepTitle   = "Compression Rate",
            scenarioContext = "You are delivering chest compressions. An AED has been requested.",
            questionText = "What is the target compression rate for adult CPR?",
            options = listOf(
                CprTrainingOption("A", "60–80 compressions per minute (about one per second).",
                    "Incorrect. 60–80 BPM is too slow to maintain circulation. The correct rate is 100–120 compressions per minute.",
                    isCorrect = false),
                CprTrainingOption("B", "100–120 compressions per minute.",
                    "Correct. The target rate is 100–120 compressions per minute. A helpful rhythm is the chorus of 'Stayin' Alive'. Too slow or too fast both reduce CPR effectiveness. (ERC Guidelines 2021 / ILCOR 2020)",
                    isCorrect = true),
                CprTrainingOption("C", "140–160 compressions per minute — faster is always better.",
                    "Incorrect. Rates above 120 BPM reduce compression depth and allow inadequate chest recoil, which diminishes cardiac output.",
                    isCorrect = false),
                CprTrainingOption("D", "There is no specific rate — just compress as fast as possible.",
                    "Incorrect. Both rate and depth matter. Compressions outside 100–120 BPM are less effective. Quality and consistency count.",
                    isCorrect = false)
            )
        )
    )

    fun stepFor(stepIndex: Int): CprTrainingStep? = steps.getOrNull(stepIndex)

    fun allSkillTags(): List<String> = steps.map { it.skillTag }.distinct()
}
