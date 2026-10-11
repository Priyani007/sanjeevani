package com.sanjeevani.training

/**
 * 7-step interactive allergic reaction / anaphylaxis training content.
 * Based on Anaphylaxis Campaign UK / ACAAI guidelines for bystander response.
 *
 * Safety note: a symptom quiz cannot establish that an allergic reaction is safe.
 * These scenarios train recognition and the correct escalation response.
 */
object AllergyTrainingContent {

    private const val MODULE = "ALLERGIC_REACTION"

    val steps: List<ModuleTrainingStep> = listOf(

        // ── Step 0: Recognise anaphylaxis ─────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 0,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.ANAPHYLAXIS_RECOGNITION,
            skillDescription = "Recognising anaphylaxis / severe allergic reaction",
            stepTitle        = "Recognise Anaphylaxis",
            scenarioContext  = "Minutes after eating at a restaurant, a person develops a raised red rash across their neck and chest, swelling of the lips, and says their throat feels tight. Their voice sounds hoarse.",
            questionText     = "What is happening, and how severe is this situation?",
            options = listOf(
                ModuleTrainingOption("A", "This is a mild allergic reaction — give an antihistamine and monitor.",
                    "Incorrect. Throat tightness, hoarse voice, and lip swelling indicate the airway may be compromised. This is anaphylaxis — a life-threatening emergency requiring epinephrine (EpiPen) and immediate 112 call.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "This is anaphylaxis — a severe, potentially life-threatening allergic reaction. Call 112 immediately.",
                    "Correct. Throat tightness, hoarse voice, rash, and lip swelling together indicate anaphylaxis. This is an airway emergency. Call 112 immediately and ask whether the person has an adrenaline (EpiPen) auto-injector. (Anaphylaxis Campaign / ACAAI)",
                    isCorrect = true),
                ModuleTrainingOption("C", "This is hives (urticaria) — uncomfortable but not dangerous. Apply a cold compress.",
                    "Incorrect. Isolated hives can be mild, but the combination of throat tightness, hoarse voice, and airway swelling indicates anaphylaxis, not simple urticaria.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "They are having a panic attack from the rash — reassure them and control their breathing.",
                    "Incorrect. Panic attack does not cause rash, lip swelling, or a hoarse voice from airway swelling. These are physical signs of anaphylaxis requiring urgent medical intervention.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 1: Call 112 ──────────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 1,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.CALL_112,
            skillDescription = "Call 112 and use EpiPen if available",
            stepTitle        = "Call 112",
            scenarioContext  = "You have recognised anaphylaxis. The person is becoming increasingly distressed. They mention they have an EpiPen prescribed to them in their bag.",
            questionText     = "What is the correct order of actions?",
            options = listOf(
                ModuleTrainingOption("A", "First administer the EpiPen, then call 112 afterwards.",
                    "Not the best answer. Do not turn these into a long sequence: use the prescribed auto-injector without delay while you or another bystander calls 112.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Call 112 immediately, and use the EpiPen without delay — do both as fast as possible.",
                    "Correct. In practice: call 112 and administer the EpiPen as simultaneously as possible. Epinephrine is the first-line treatment; 112 ensures professional backup. Do not delay either action. (Anaphylaxis Campaign UK / WAO)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Locate an antihistamine first — this will reduce the allergic response quickly.",
                    "Incorrect. Antihistamines are too slow to treat anaphylaxis. Only epinephrine works fast enough to prevent airway closure. Administer the EpiPen and call 112.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Wait for symptoms to worsen before calling 112 — it may resolve on its own.",
                    "Incorrect. Anaphylaxis can become fatal rapidly. Waiting for worsening delays potentially life-saving treatment. Act immediately.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 2: Lay flat ──────────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 2,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.LAY_FLAT,
            skillDescription = "Position: lay flat with legs raised (unless breathing is worse)",
            stepTitle        = "Position the Person",
            scenarioContext  = "You have called 112. The person is conscious but feeling faint and weak. They want to sit up.",
            questionText     = "How should you position someone with anaphylaxis who feels faint?",
            cameraRequirement = ModuleCameraRequirement.PATIENT_LYING_FLAT,
            options = listOf(
                ModuleTrainingOption("A", "Let them sit upright — they will feel more comfortable and breathe more easily.",
                    "Incorrect. Sitting upright causes a sudden drop in blood pressure that can be fatal in anaphylaxis (empty vena cava / empty ventricle syndrome). Lay them flat.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Lay them flat on their back with legs raised — unless they are struggling to breathe.",
                    "Correct. Flat with legs raised counteracts the severe drop in blood pressure during anaphylaxis. Do NOT allow them to sit or stand up suddenly. Exception: if breathing is worse lying down, allow them to sit. If unconscious but breathing: use the recovery position. (Anaphylaxis Campaign / WAO Guidelines)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Stand them up and walk them around to maintain circulation.",
                    "Incorrect. Standing causes blood to pool in the legs, worsening the hypotension of anaphylaxis. This can be fatal. Lay them flat.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Position them on their side in the recovery position regardless of their condition.",
                    "Incorrect. The recovery position is for unconscious breathing patients. Conscious patients in anaphylaxis with low blood pressure should be flat with legs raised.",
                    isCorrect = false)
            )
        ),

        // ── Step 3: EpiPen location ───────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 3,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.EPIPEN_LOCATION,
            skillDescription = "Where to inject the EpiPen auto-injector",
            stepTitle        = "EpiPen Injection Site",
            scenarioContext  = "You have the EpiPen in your hand. The person is lying flat, conscious and cooperative.",
            questionText     = "Where is the correct injection site for an adrenaline auto-injector (EpiPen)?",
            options = listOf(
                ModuleTrainingOption("A", "The upper arm, like a typical vaccination.",
                    "Incorrect. The upper arm is used for some vaccines but is not the target for EpiPen. The outer mid-thigh gives the fastest absorption.",
                    isCorrect = false),
                ModuleTrainingOption("B", "The outer middle of one thigh — can be injected through clothing.",
                    "Correct. The outer mid-thigh (approximately midway between hip and knee) is the correct site. It can be administered through clothing. Fastest muscle absorption occurs here. (Anaphylaxis Campaign UK / AAAAI)",
                    isCorrect = true),
                ModuleTrainingOption("C", "The abdomen, two finger-widths from the navel.",
                    "Incorrect. The abdomen is used for some medications (e.g. insulin), but not for EpiPen, which targets the outer thigh for muscle absorption.",
                    isCorrect = false),
                ModuleTrainingOption("D", "The inner thigh, as it has the most accessible blood vessels.",
                    "Incorrect. The OUTER mid-thigh is the correct site. The inner thigh is not used — it does not provide the recommended intramuscular target.",
                    isCorrect = false)
            ),
            requiresPlacementSimulation = true,
            cameraRequirement = ModuleCameraRequirement.THIGH_REFERENCE_VISIBLE
        ),

        // ── Step 4: EpiPen technique ──────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 4,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.EPIPEN_TECHNIQUE,
            skillDescription = "EpiPen auto-injector technique",
            stepTitle        = "EpiPen Technique",
            scenarioContext  = "You are ready to administer the EpiPen to the outer mid-thigh.",
            questionText     = "What is the correct technique for using the EpiPen?",
            options = listOf(
                ModuleTrainingOption("A", "Follow the specific device label: remove its safety release, press the needle end firmly against the outer mid-thigh, and hold for the labelled time.",
                    "Correct. Auto-injector designs and hold times differ. Follow the label on that prescribed device, use the outer mid-thigh, and call 112. Practise only with a trainer device or this diagram — never inject a person for training.",
                    isCorrect = true),
                ModuleTrainingOption("B", "Remove both ends, shake the device, then push the needle manually into the thigh.",
                    "Incorrect. The EpiPen is an auto-injector — it deploys automatically when pressed firmly. Never attempt manual operation or shake the device.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("C", "Hold the needle end pointing up and press it on the upper arm for five seconds.",
                    "Incorrect. The needle end goes firmly against the outer mid-thigh. Follow the labelled hold time for the specific device.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Inject into the nearest visible vein for the fastest effect.",
                    "Incorrect. EpiPen is an intramuscular injection — into the muscle of the outer thigh. Injecting into a vein is extremely dangerous. Never attempt intravenous injection.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 5: After injecting ───────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 5,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.POST_INJECTION,
            skillDescription = "Actions after EpiPen administration",
            stepTitle        = "After EpiPen Use",
            scenarioContext  = "You have administered the EpiPen. The person says they feel slightly better after a couple of minutes.",
            questionText     = "Symptoms seem to improve after EpiPen. What do you do next?",
            options = listOf(
                ModuleTrainingOption("A", "Cancel the 112 call — the EpiPen has worked and the emergency is over.",
                    "Incorrect. Keep the 112 response active even if symptoms improve; symptoms can return and medical assessment is still required.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("B", "Keep them lying flat, maintain the 112 call, monitor breathing and consciousness, and prepare to use a second EpiPen if available.",
                    "Correct. Stay with them, monitor breathing and consciousness, and follow the dispatcher and the person's emergency plan/device instructions about a second dose. Do not cancel emergency care because they briefly improve.",
                    isCorrect = true),
                ModuleTrainingOption("C", "Give them antihistamines orally now that initial anaphylaxis is controlled.",
                    "Incorrect. Oral antihistamines are ineffective in active anaphylaxis and take too long to work. Post-EpiPen management is in hospital, not at home.",
                    isCorrect = false),
                ModuleTrainingOption("D", "Let the person sit up and eat something to recover their strength.",
                    "Incorrect. Sitting up may cause dangerous blood pressure drop. Food and drink are contraindicated — swallowing reflex may still be impaired. Keep them flat and wait for the ambulance.",
                    isCorrect = false, isSafe = false)
            )
        ),

        // ── Step 6: Monitoring ────────────────────────────────────────────────
        ModuleTrainingStep(
            stepIndex        = 6,
            moduleType       = MODULE,
            skillTag         = AllergySkillTag.MONITORING,
            skillDescription = "Monitoring and handover to emergency services",
            stepTitle        = "Monitoring",
            scenarioContext  = "Emergency services are 5 minutes away. The person is lying flat; breathing appears laboured.",
            questionText     = "What information should you give the 112 dispatcher and how do you monitor?",
            options = listOf(
                ModuleTrainingOption("A", "Only state the location — the paramedics will assess everything when they arrive.",
                    "Incorrect. Providing key information helps paramedics prepare the right equipment and treatment en route, reducing time to definitive care.",
                    isCorrect = false),
                ModuleTrainingOption("B", "Tell the dispatcher: the person's known allergen (if known), time of exposure, symptoms, whether EpiPen was given and when, and current condition. Monitor breathing and consciousness every 30–60 seconds.",
                    "Correct. Critical handover information: suspected allergen and exposure time, symptoms and their progression, whether adrenaline was given and time administered, current breathing and consciousness level. Continue monitoring until paramedics arrive. If unconscious and not breathing: begin CPR. (Anaphylaxis Campaign / WAO)",
                    isCorrect = true),
                ModuleTrainingOption("C", "Stop monitoring once the EpiPen has been given — the medication is managing the reaction.",
                    "Incorrect. Anaphylaxis can worsen or lead to cardiac arrest even after EpiPen use. Active monitoring until paramedics arrive is essential.",
                    isCorrect = false, isSafe = false),
                ModuleTrainingOption("D", "Ask bystanders to search online for the allergen to inform the paramedics.",
                    "Incorrect. Use your time monitoring the patient and staying on the line with 112 dispatch. Searching online delays your attention.",
                    isCorrect = false)
            )
        )
    )

    fun stepFor(stepIndex: Int): ModuleTrainingStep? = steps.getOrNull(stepIndex)

    fun allSkillTags(): List<String> = steps.map { it.skillTag }.distinct()
}
